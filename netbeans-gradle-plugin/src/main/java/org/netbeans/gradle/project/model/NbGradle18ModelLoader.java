package org.netbeans.gradle.project.model;

import java.io.File;
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
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectExtensionRef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class NbGradle18ModelLoader implements NbModelLoader {
    private final LongRunningOperationSetup setup;

    public NbGradle18ModelLoader(LongRunningOperationSetup setup) {
        if (setup == null) throw new NullPointerException("setup");

        this.setup = setup;
    }

    @SuppressWarnings("unchecked")
    private static Class<BuildAction<?>> buildActionClass() {
        return (Class<BuildAction<?>>)(Class<?>)BuildAction.class;
    }

    private ModelLoaderAction getBuildAction(List<ProjectExtensionRef> extensionRefs) {
        List<List<Class<?>>> requestedModels = new LinkedList<List<Class<?>>>();
        requestedModels.add(Collections.<Class<?>>singletonList(IdeaModule.class));
        requestedModels.add(Collections.<Class<?>>singletonList(IdeaProject.class));
        requestedModels.add(Collections.<Class<?>>singletonList(GradleProject.class));
        requestedModels.add(Collections.<Class<?>>singletonList(EclipseProject.class));

        for (ProjectExtensionRef extensionRef: extensionRefs) {
            GradleProjectExtension extension = extensionRef.getExtension();

            Iterable<List<Class<?>>> extensionModels = extension.getGradleModels();
            for (List<Class<?>> extensionModel: extensionModels) {
                requestedModels.add(extensionModel);
            }
        }

        return new ModelLoaderAction(requestedModels);
    }

    private NbGradleModel getNBModel(Models models) throws IOException {
        Lookup modelLookup = Lookups.fixed(models.getModels().toArray());
        IdeaProject ideaProject = modelLookup.lookup(IdeaProject.class);
        EclipseProject eclipseProject = modelLookup.lookup(EclipseProject.class);
        if (ideaProject == null || eclipseProject == null) {
            return null;
        }

        IdeaModule module = GradleModelLoader.tryFindMainModule(
                eclipseProject.getProjectDirectory(), ideaProject);

        return module != null
                ? NbCompatibleModelLoader.loadMainModelFromIdeaModule(module)
                : null;
    }

    @Override
    public Result loadModels(
            NbGradleProject project,
            ProjectConnection connection,
            ProgressHandle progress) throws IOException {

        List<ProjectExtensionRef> extensionRefs = project.getExtensionRefs();
        ModelLoaderAction buildAction = getBuildAction(extensionRefs);

        BuildActionExecuter<ModelsForAll> executer = connection.action(buildAction);
        setup.setupLongRunningOperation(executer);

        ModelsForAll loadedModelsForAll = executer.run();
        NbGradleModel mainModel = getNBModel(loadedModelsForAll.getModelForDefaultProject());
        if (mainModel == null) {
            throw new IOException("Failed to load required model classes for " + project.getProjectDirectory());
        }

        loadModelsForExtensions(mainModel, loadedModelsForAll.getModelForDefaultProject(), extensionRefs);

        List<NbGradleModel> otherModels = new LinkedList<NbGradleModel>();
        for (Map.Entry<File, Models> projectEntry: loadedModelsForAll.getProjectModels().entrySet()) {
            NbGradleModel otherModel = getNBModel(projectEntry.getValue());
            if (otherModel != null) {
                loadModelsForExtensions(otherModel, projectEntry.getValue(), extensionRefs);
                otherModels.add(otherModel);
            }
        }

        return new Result(mainModel, otherModels);
    }

    private static void loadModelsForExtensions(
            NbGradleModel nbModel,
            Models foundModels,
            List<ProjectExtensionRef> extensionRefs) {

        Lookup modelLookup = Lookups.fixed(foundModels.getModels().toArray());

        for (ProjectExtensionRef extensionRef: extensionRefs) {
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

            nbModel.setModelsForExtension(extensionRef.getName(), Lookups.fixed(extensionLookupContent.toArray()));
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

    private static class ModelLoaderAction implements BuildAction<NbGradle18ModelLoader.ModelsForAll> {
        private static final long serialVersionUID = 8168780059406328344L;
        private final List<List<Class<?>>> models;

        public ModelLoaderAction(List<List<Class<?>>> models) {
            this.models = new ArrayList<List<Class<?>>>(models.size());
            for (List<Class<?>> modelsForEntity: models) {
                this.models.add(new ArrayList<Class<?>>(modelsForEntity));
            }
        }

        private Object loadModel(
                NbGradle18ModelLoader.ModelFinder modelFinder,
                Class<?> modelClass,
                List<Object> result,
                Map<Class<?>, Object> resultMap) {

            Object model = resultMap.get(modelClass);
            if (model != null) {
                return model;
            }

            if (modelClass == IdeaProject.class) {
                IdeaModule ideaModule = (IdeaModule)loadModel(modelFinder, IdeaModule.class, result, resultMap);
                if (ideaModule == null) {
                    model = modelFinder.tryGetModel(modelClass);
                }
                else {
                    model = ideaModule.getProject();
                }
            }
            else {
                model = modelFinder.tryGetModel(modelClass);
            }

            if (model != null) {
                result.add(model);
                resultMap.put(modelClass, model);
            }
            return model;
        }

        private NbGradle18ModelLoader.Models findModels(NbGradle18ModelLoader.ModelFinder modelFinder) {
            List<Object> result = new ArrayList<Object>(models.size());
            Map<Class<?>, Object> loadedModels = new IdentityHashMap<Class<?>, Object>(2 * models.size());

            for (List<Class<?>> modelsForEntity: models) {
                for (Class<?> modelClass: modelsForEntity) {
                    Object model = loadModel(modelFinder, modelClass, result, loadedModels);
                    if (model != null) {
                        break;
                    }
                }
            }
            return new NbGradle18ModelLoader.Models(result);
        }

        private <T> T getModel(Class<T> modelClass, NbGradle18ModelLoader.ModelFinder modelFinder, NbGradle18ModelLoader.Models loadedModels) {
            Lookup modelLookup = Lookups.fixed(loadedModels.getModels().toArray());
            T result = modelLookup.lookup(modelClass);
            if (result == null) {
                result = modelFinder.tryGetModel(modelClass);
            }
            return result;
        }

        @Override
        public NbGradle18ModelLoader.ModelsForAll execute(final BuildController controller) {
            NbGradle18ModelLoader.ModelFinder mainModelFinder = new NbGradle18ModelLoader.ModelFinder() {
                @Override
                public <T> T tryGetModel(Class<T> modelClass) {
                    return controller.findModel(modelClass);
                }
            };
            NbGradle18ModelLoader.Models modelsForDefault = findModels(mainModelFinder);
            IdeaModule mainIdeaModule = getModel(IdeaModule.class, mainModelFinder, modelsForDefault);
            File mainModuleDir = mainIdeaModule != null
                    ? GradleModelLoader.tryGetModuleDir(mainIdeaModule)
                    : null;

            Collection<? extends BasicGradleProject> projects = controller.getBuildModel().getProjects();
            Map<File, NbGradle18ModelLoader.Models> projectModels = new HashMap<File, NbGradle18ModelLoader.Models>(2 * projects.size());
            for (final BasicGradleProject project: projects) {
                NbGradle18ModelLoader.ModelFinder otherModelFinder = new NbGradle18ModelLoader.ModelFinder() {
                    @Override
                    public <T> T tryGetModel(Class<T> modelClass) {
                        return controller.findModel(project, modelClass);
                    }
                };

                NbGradle18ModelLoader.Models otherModels = findModels(otherModelFinder);
                EclipseProject eclipseProject = getModel(EclipseProject.class, mainModelFinder, otherModels);
                if (eclipseProject != null) {
                    File moduleDir = eclipseProject.getProjectDirectory();
                    if (moduleDir != null && !moduleDir.equals(mainModuleDir)) {
                        projectModels.put(moduleDir, otherModels);
                    }
                }
//                IdeaModule ideaModule = getModel(IdeaModule.class, mainModelFinder, otherModels);
//                if (ideaModule != null) {
//                    File moduleDir = GradleModelLoader.tryGetModuleDir(ideaModule);
//                    if (moduleDir != null && !moduleDir.equals(mainModuleDir)) {
//                        projectModels.put(moduleDir, otherModels);
//                    }
//                }
            }
            return new NbGradle18ModelLoader.ModelsForAll(modelsForDefault, projectModels);
        }
    }

    private static final class ModelsForAll implements Serializable {
        private static final long serialVersionUID = 1327087236723573084L;

        private final Models modelForDefaultProject;
        private final Map<File, Models> projectModels;

        public ModelsForAll(Models modelForDefaultProject, Map<File, Models> projectModels) {
            if (modelForDefaultProject == null) throw new NullPointerException("modelForDefaultProject");
            if (projectModels == null) throw new NullPointerException("projectModels");

            this.modelForDefaultProject = modelForDefaultProject;
            this.projectModels = CollectionUtils.copyNullSafeHashMap(projectModels);
        }

        public Models getModelForDefaultProject() {
            return modelForDefaultProject;
        }

        public Map<File, Models> getProjectModels() {
            return projectModels;
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

    public interface ModelFinder {
        public <T> T tryGetModel(Class<T> modelClass);
    }
}
