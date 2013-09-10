package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.UnknownModelException;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleMultiProjectDef;
import org.netbeans.gradle.model.GradleProjectTree;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.model.OperationInitializer;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectExtensionRef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class NbCompatibleModelLoader implements NbModelLoader {
    private static final Logger LOGGER = Logger.getLogger(NbCompatibleModelLoader.class.getName());

    private final NbGradleModel proposedModel;
    private final OperationInitializer setup;

    public NbCompatibleModelLoader(NbGradleModel proposedModel, OperationInitializer setup) {
        if (setup == null) throw new NullPointerException("setup");

        this.proposedModel = proposedModel;
        this.setup = setup;
    }

    @Override
    public Result loadModels(
            NbGradleProject project,
            ProjectConnection connection,
            ProgressHandle progress) throws IOException {

        List<NbGradleModel> otherModels = new LinkedList<NbGradleModel>();
        NbGradleModel mainModel = proposedModel;

        if (mainModel == null) {
            mainModel = loadMainModel(project, connection, otherModels);
        }

        getExtensionModels(project, connection, mainModel);

        return new Result(mainModel, otherModels);
    }

    private <T> T getModelWithProgress(
            ProjectConnection projectConnection,
            Class<T> model) {
        ModelBuilder<T> builder = projectConnection.model(model);
        GradleModelLoader.setupLongRunningOP(setup, builder);

        return builder.get();
    }

    private void getExtensionModels(
            NbGradleProject project,
            ProjectConnection projectConnection,
            NbGradleModel result) {

        Lookup allModels = result.getAllModels();
        for (ProjectExtensionRef extensionRef: result.getUnloadedExtensions(project)) {
            GradleProjectExtension extension = extensionRef.getExtension();
            List<Object> extensionModels = new LinkedList<Object>();

            for (List<Class<?>> modelRequest: extension.getGradleModels()) {
                for (Class<?> modelClass: modelRequest) {
                    try {
                        Object model = allModels.lookup(modelClass);
                        if (model == null) {
                            model = getModelWithProgress(projectConnection, modelClass);
                        }
                        extensionModels.add(model);
                        break;
                    } catch (UnknownModelException ex) {
                        Throwable loggedException = LOGGER.isLoggable(Level.FINE)
                                ? ex
                                : null;
                        LOGGER.log(Level.INFO, "Cannot find model " + modelClass.getName(), loggedException);
                    }
                }
            }

            result.setModelsForExtension(extensionRef.getName(), Lookups.fixed(extensionModels.toArray()));
        }
    }

    private static List<GradleTaskID> getTasksOfModule(IdeaModule module) {
        DomainObjectSet<? extends GradleTask> modelTasks = module.getGradleProject().getTasks();
        List<GradleTaskID> result = new ArrayList<GradleTaskID>(modelTasks.size());

        for (GradleTask modelTask: modelTasks) {
            result.add(new GradleTaskID(modelTask.getName(), modelTask.getPath()));
        }
        return result;
    }

    private static GradleProjectTree tryCreateProjectTreeFromIdea(IdeaModule module) {
        File moduleDir = GradleModelLoader.tryGetModuleDir(module);
        if (moduleDir == null) {
            return null;
        }

        int expectedChildCount = module.getGradleProject().getChildren().size();
        List<GradleProjectTree> children = new ArrayList<GradleProjectTree>(expectedChildCount);
        for (IdeaModule child: GradleModelLoader.getChildModules(module)) {
            GradleProjectTree childInfo = tryCreateProjectTreeFromIdea(child);
            if (childInfo != null) {
                children.add(childInfo);
            }
        }

        GradleProject gradleProject = module.getGradleProject();
        String projectName = gradleProject.getName();
        String projectFullName = gradleProject.getPath();
        GenericProjectProperties properties
                = new GenericProjectProperties(projectName, projectFullName, moduleDir);

        return new GradleProjectTree(properties, getTasksOfModule(module), children);
    }

    private static NbGradleModel loadMainModelFromIdeaModule(
            GradleProjectTree rootProject,
            IdeaModule ideaModule) throws IOException {
        if (rootProject == null) throw new NullPointerException("rootProject");
        if (ideaModule == null) throw new NullPointerException("ideaModule");

        GradleProjectTree projectTree = tryCreateProjectTreeFromIdea(ideaModule);
        if (projectTree == null) {
            throw new IOException("Failed to create project tree for project: " + ideaModule.getName());
        }

        NbGradleModel result = new NbGradleModel(new GradleMultiProjectDef(rootProject, projectTree));
        result.setMainModels(Lookups.fixed(ideaModule, ideaModule.getProject()));
        return result;
    }

    public static NbGradleModel loadMainModelFromIdeaModule(IdeaModule ideaModule) throws IOException {
        // TODO: Remove this method once it is no longer needed.

        GradleProjectTree projectTree = tryCreateProjectTreeFromIdea(ideaModule);
        if (projectTree == null) {
            throw new IOException("Failed to create project tree for project: " + ideaModule.getName());
        }

        IdeaModule rootModule = GradleModelLoader.tryFindRootModule(ideaModule.getProject());
        if (rootModule == null) {
            throw new IOException("Failed to find root module for project: " + ideaModule.getName());
        }

        GradleProjectTree root = tryCreateProjectTreeFromIdea(rootModule);
        if (root == null) {
            throw new IOException("Failed to find root tree for project: " + ideaModule.getName());
        }

        NbGradleModel result = new NbGradleModel(new GradleMultiProjectDef(root, projectTree));
        result.setMainModels(Lookups.fixed(ideaModule, ideaModule.getProject()));
        return result;
    }

    private NbGradleModel loadMainModel(
            NbGradleProject project,
            ProjectConnection projectConnection,
            List<NbGradleModel> deduced) throws IOException {

        IdeaProject ideaProject
                = getModelWithProgress(projectConnection, IdeaProject.class);

        return parseMainModel(project, ideaProject, deduced);
    }

    public static NbGradleModel parseMainModel(
            NbGradleProject project,
            IdeaProject ideaProject,
            List<? super NbGradleModel> deduced) throws IOException {
        if (project == null) throw new NullPointerException("project");
        if (ideaProject == null) throw new NullPointerException("ideaProject");
        if (deduced == null) throw new NullPointerException("deduced");

        File projectDir = project.getProjectDirectoryAsFile();
        IdeaModule mainModule = GradleModelLoader.tryFindMainModule(projectDir, ideaProject);
        if (mainModule == null) {
            throw new IOException("Failed to find idea module for project: " + project.getDisplayName());
        }

        IdeaModule rootModule = GradleModelLoader.tryFindRootModule(ideaProject);
        if (rootModule == null) {
            throw new IOException("Failed to find root module for project: " + project.getDisplayName());
        }

        GradleProjectTree rootTree = tryCreateProjectTreeFromIdea(rootModule);
        if (rootTree == null) {
            throw new IOException("Failed to find root tree for project: " + rootModule.getName());
        }

        String rootPath = rootModule.getGradleProject().getPath();
        for (IdeaModule otherModule: ideaProject.getModules()) {
            // This comparison is not strictly necessary but there is no reason
            // to reparse the main project.
            if (otherModule != mainModule) {
                if (rootPath.equals(otherModule.getGradleProject().getPath())) {
                    deduced.add(new NbGradleModel(new GradleMultiProjectDef(rootTree, rootTree)));
                }
                else {
                    deduced.add(loadMainModelFromIdeaModule(rootTree, otherModule));
                }
            }
        }

        GradleProjectTree mainTree;
        if (rootPath.equals(mainModule.getGradleProject().getPath())) {
            mainTree = rootTree;
        }
        else {
            mainTree = tryCreateProjectTreeFromIdea(mainModule);
        }

        if (mainTree == null) {
            throw new IOException("Failed to find tree for project: " + mainModule.getName());
        }

        return new NbGradleModel(new GradleMultiProjectDef(rootTree, mainTree));
    }
}
