package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.UnknownModelException;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectExtensionRef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class NbGradle18ModelLoader implements NbModelLoader {
    private final NbGradleModel proposedModel;
    private final LongRunningOperationSetup setup;

    public NbGradle18ModelLoader(NbGradleModel proposedModel, LongRunningOperationSetup setup) {
        if (setup == null) throw new NullPointerException("setup");

        this.proposedModel = proposedModel;
        this.setup = setup;
    }

    @SuppressWarnings("unchecked")
    private static Class<BuildAction<?>> buildActionClass() {
        return (Class<BuildAction<?>>)(Class<?>)BuildAction.class;
    }

    private ModelLoaderAction getBuildAction(List<ProjectExtensionRef> extensionRefs) {
        List<List<Class<?>>> requestedModels = new LinkedList<List<Class<?>>>();
        requestedModels.add(Collections.<Class<?>>singletonList(IdeaProject.class));

        Map<String, Collection<BuildAction<?>>> extensionActions
                = new HashMap<String, Collection<BuildAction<?>>>(2 * extensionRefs.size());

        for (ProjectExtensionRef extensionRef: extensionRefs) {
            GradleProjectExtension extension = extensionRef.getExtension();

            Iterable<List<Class<?>>> extensionModels = extension.getGradleModels();
            for (List<Class<?>> extensionModel: extensionModels) {
                requestedModels.add(extensionModel);
            }

            String extensionName = extension.getExtensionName();

            Collection<? extends BuildAction<?>> actions
                    = extension.getExtensionLookup().lookupAll(buildActionClass());

            extensionActions.put(extensionName, new ArrayList<BuildAction<?>>(actions));
        }

        return new ModelLoaderAction(requestedModels, extensionActions);
    }

    @Override
    public Result loadModels(
            NbGradleProject project,
            ProjectConnection connection,
            ProgressHandle progress) throws IOException {

        NbGradleModel mainModel = proposedModel;

        List<ProjectExtensionRef> extensionRefs;
        if (mainModel != null) {
            extensionRefs = mainModel.getUnloadedExtensions(project);
        }
        else {
            extensionRefs = project.getExtensionRefs();
        }

        ModelLoaderAction buildAction = getBuildAction(extensionRefs);

        BuildActionExecuter<Models> executer = connection.action(buildAction);
        setup.setupLongRunningOperation(executer);

        Models loadedModels = executer.run();
        Lookup modelLookup = Lookups.fixed(loadedModels.getModels().toArray());

        IdeaProject mainIdeaProject = modelLookup.lookup(IdeaProject.class);
        if (mainIdeaProject == null) {
            throw new IOException("Failed to load IdeaProject for " + project.getProjectDirectory());
        }

        List<NbGradleModel> otherModels = new LinkedList<NbGradleModel>();

        if (mainModel == null) {
            mainModel = NbCompatibleModelLoader.parseMainModel(project, mainIdeaProject, otherModels);
        }

        loadModelsForExtensions(project, mainModel, loadedModels.getBuildActionObjects(), modelLookup);

        return new Result(mainModel, otherModels);
    }

    private static void loadModelsForExtensions(
            NbGradleProject project,
            NbGradleModel mainModel,
            Map<String, Collection<Object>> extensionObjects,
            Lookup modelLookup) {

        for (ProjectExtensionRef extensionRef: mainModel.getUnloadedExtensions(project)) {
            GradleProjectExtension extension = extensionRef.getExtension();

            Iterable<List<Class<?>>> extensionModels = extension.getGradleModels();
            List<Object> extensionLookupContent = new LinkedList<Object>();

            for (List<Class<?>> extensionModelPrefList: extensionModels) {
                for (Class<?> modelClass: extensionModelPrefList) {
                    Object model = modelLookup.lookup(modelClass);
                    if (model != null) {
                        extensionLookupContent.add(model);
                        break;
                    }
                }
            }
            Collection<Object> customObjects = extensionObjects.get(extension.getExtensionName());

            if (customObjects != null) {
                extensionLookupContent.addAll(customObjects);
            }

            mainModel.setModelsForExtension(extensionRef, Lookups.fixed(extensionLookupContent.toArray()));
        }
    }

    private static <K, V> Map<K, Collection<V>> copyMultiValueMap(Map<K, Collection<V>> source) {
        Map<K, Collection<V>> result
                = new HashMap<K, Collection<V>>(2 * source.size());

        for (Map.Entry<K, Collection<V>> entry: source.entrySet()) {
            result.put(entry.getKey(), CollectionUtils.copyNullSafeList(entry.getValue()));
        }

        return Collections.unmodifiableMap(result);
    }

    private static final class Models implements Serializable {
        private static final long serialVersionUID = 6282985709430938034L;

        private final List<Object> models;
        private final Map<String, Collection<Object>> buildActionObjects;

        public Models(List<?> models, Map<String, Collection<Object>> buildActionObjects) {
            this.models = CollectionUtils.copyNullSafeList(models);
            this.buildActionObjects = copyMultiValueMap(buildActionObjects);
        }

        public List<Object> getModels() {
            return models;
        }

        public Map<String, Collection<Object>> getBuildActionObjects() {
            return buildActionObjects;
        }
    }

    private static class ModelLoaderAction implements BuildAction<Models> {
        private static final long serialVersionUID = 8168780059406328344L;

        private final List<List<Class<?>>> models;
        private final Map<String, Collection<BuildAction<?>>> buildActions;

        public ModelLoaderAction(List<List<Class<?>>> models, Map<String, Collection<BuildAction<?>>> buildActions) {
            this.models = new ArrayList<List<Class<?>>>(models.size());

            for (List<Class<?>> modelsForEntity: models) {
                this.models.add(new ArrayList<Class<?>>(modelsForEntity));
            }

            this.buildActions = copyMultiValueMap(buildActions);
        }

        @Override
        public Models execute(BuildController controller) {
            List<Object> result = new ArrayList<Object>(models.size());
            Map<Class<?>, Object> loadedModels = new IdentityHashMap<Class<?>, Object>(2 * models.size());

            for (List<Class<?>> modelsForEntity: models) {
                for (Class<?> modelClass: modelsForEntity) {
                    try {
                        Object model = loadedModels.get(modelClass);
                        if (model == null) {
                            model = controller.getModel(modelClass);
                        }

                        if (model != null) {
                            loadedModels.put(modelClass, model);
                            result.add(model);
                            break;
                        }
                    } catch (UnknownModelException ex) {
                        // Try the next model in the list.
                    }
                }
            }

            Map<String, Collection<Object>> actionResults
                    = new HashMap<String, Collection<Object>>(2 * buildActions.size());

            for (Map.Entry<String, Collection<BuildAction<?>>> entry: buildActions.entrySet()) {
                Collection<BuildAction<?>> actions = entry.getValue();
                List<Object> extensionResults = new ArrayList<Object>(actions.size());

                for (BuildAction<?> action: actions) {
                    Object actionResult = action.execute(controller);
                    if (actionResult != null) {
                        extensionResults.add(actionResult);
                    }
                }
                actionResults.put(entry.getKey(), extensionResults);
            }

            return new Models(result, actionResults);
        }
    }
}
