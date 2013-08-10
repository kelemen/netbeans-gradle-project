package org.netbeans.gradle.project.model;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
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

        List<List<Class<?>>> requestedModels = new LinkedList<List<Class<?>>>();
        requestedModels.add(Collections.<Class<?>>singletonList(IdeaProject.class));

        for (ProjectExtensionRef extensionRef: extensionRefs) {
            Iterable<List<Class<?>>> extensionModels = extensionRef.getExtension().getGradleModels();
            for (List<Class<?>> extensionModel: extensionModels) {
                requestedModels.add(extensionModel);
            }
        }

        BuildActionExecuter<Models> executer = connection.action(new ModelLoaderAction(requestedModels));
        setup.setupLongRunningOperation(executer);

        Models loadedModels = executer.run();
        Lookup modelLookup = Lookups.fixed(loadedModels.getModels().toArray());

        IdeaProject mainIdeaProject = modelLookup.lookup(IdeaProject.class);
        List<NbGradleModel> otherModels = new LinkedList<NbGradleModel>();

        if (mainModel == null) {
            mainModel = NbCompatibleModelLoader.parseMainModel(project, mainIdeaProject, otherModels);
        }

        loadModelsForExtensions(project, mainModel, modelLookup);

        return new Result(mainModel, otherModels);
    }

    private static void loadModelsForExtensions(
            NbGradleProject project,
            NbGradleModel mainModel,
            Lookup modelLookup) {

        for (ProjectExtensionRef extensionRef: mainModel.getUnloadedExtensions(project)) {
            Iterable<List<Class<?>>> extensionModels = extensionRef.getExtension().getGradleModels();
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

            mainModel.setModelsForExtension(extensionRef, Lookups.fixed(extensionLookupContent.toArray()));
        }
    }

    private static final class Models implements Serializable {
        private static final long serialVersionUID = 6282985709430938034L;

        private final List<Object> models;

        public Models(List<?> models) {
            this.models = CollectionUtils.copyNullSafeList(models);
        }

        public List<Object> getModels() {
            return models;
        }
    }

    private static class ModelLoaderAction implements BuildAction<Models> {
        private static final long serialVersionUID = 8168780059406328344L;

        private final List<List<Class<?>>> models;

        public ModelLoaderAction(List<List<Class<?>>> models) {
            this.models = new ArrayList<List<Class<?>>>(models.size());

            for (List<Class<?>> modelsForEntity: models) {
                this.models.add(new ArrayList<Class<?>>(modelsForEntity));
            }
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

            return new Models(result);
        }
    }
}
