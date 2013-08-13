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
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectExtensionRef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class NbCompatibleModelLoader implements NbModelLoader {
    private static final Logger LOGGER = Logger.getLogger(NbCompatibleModelLoader.class.getName());

    private final NbGradleModel proposedModel;
    private final LongRunningOperationSetup setup;

    public NbCompatibleModelLoader(NbGradleModel proposedModel, LongRunningOperationSetup setup) {
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
        setup.setupLongRunningOperation(builder);

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

            result.setModelsForExtension(extensionRef, Lookups.fixed(extensionModels.toArray()));
        }
    }

    private static GradleProjectInfo tryCreateProjectTreeFromIdea(IdeaModule module) {
        File moduleDir = GradleModelLoader.tryGetModuleDir(module);
        if (moduleDir == null) {
            return null;
        }

        int expectedChildCount = module.getGradleProject().getChildren().size();
        List<GradleProjectInfo> children = new ArrayList<GradleProjectInfo>(expectedChildCount);
        for (IdeaModule child: GradleModelLoader.getChildModules(module)) {
            GradleProjectInfo childInfo = tryCreateProjectTreeFromIdea(child);
            if (childInfo != null) {
                children.add(childInfo);
            }
        }

        return new GradleProjectInfo(module.getGradleProject(), moduleDir, children);
    }

    private static NbGradleModel loadMainModelFromIdeaModule(IdeaModule ideaModule) throws IOException {
        GradleProjectInfo projectInfo = tryCreateProjectTreeFromIdea(ideaModule);
        if (projectInfo == null) {
            throw new IOException("Failed to create project info for project: " + ideaModule.getName());
        }

        NbGradleModel result = new NbGradleModel(projectInfo, projectInfo.getProjectDir());
        result.setMainModels(Lookups.singleton(ideaModule.getProject()));
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

        for (IdeaModule otherModule: ideaProject.getModules()) {
            // This comparison is not strictly necessary but there is no reason
            // to reparse the main project.
            if (otherModule != mainModule) {
                deduced.add(loadMainModelFromIdeaModule(otherModule));
            }
        }

        return loadMainModelFromIdeaModule(mainModule);
    }
}
