package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleMultiProjectDef;
import org.netbeans.gradle.model.GradleProjectTree;

public final class NbGradleMultiProjectDef {
    private static final Logger LOGGER = Logger.getLogger(NbGradleMultiProjectDef.class.getName());

    private final NbGradleProjectTree rootProject;
    private final NbGradleProjectTree mainProject;

    public NbGradleMultiProjectDef(NbGradleProjectTree rootProject, NbGradleProjectTree mainProject) {
        if (rootProject == null) throw new NullPointerException("rootProject");
        if (mainProject == null) throw new NullPointerException("mainProject");

        this.rootProject = rootProject;
        this.mainProject = mainProject;
    }

    public NbGradleMultiProjectDef(GradleMultiProjectDef model) {
        if (model == null) throw new NullPointerException("model");

        this.rootProject = new NbGradleProjectTree(model.getRootProject());

        GradleProjectTree mainProjectModel = model.getMainProject();
        NbGradleProjectTree parsedMain = rootProject.findByPath(
                mainProjectModel.getGenericProperties().getProjectFullName());

        if (cmpTrees(parsedMain, mainProjectModel)) {
            this.mainProject = parsedMain;
        }
        else {
            LOGGER.log(Level.WARNING, "Main project tree has not been found from the root project: {0}",
                    mainProjectModel.getGenericProperties().getProjectFullName());

            this.mainProject = new NbGradleProjectTree(mainProjectModel);
        }
    }

    // Just a sanity check, not a complete recursive one.
    private static boolean cmpTrees(NbGradleProjectTree parsed, GradleProjectTree model) {
        if (parsed == null) {
            return false;
        }

        GenericProjectProperties parsedProperties = parsed.getGenericProperties();
        GenericProjectProperties modelProperties = model.getGenericProperties();
        if (!cmpTrees(parsedProperties, modelProperties)) {
            return false;
        }
        if (parsed.getTasks().size() != model.getTasks().size()) {
            return false;
        }
        if (parsed.getChildren().size() != model.getChildren().size()) {
            return false;
        }
        return true;
    }

    private static boolean cmpTrees(GenericProjectProperties prop1, GenericProjectProperties prop2) {
        if (!prop1.getProjectDir().equals(prop2.getProjectDir())) {
            return false;
        }
        if (!prop1.getProjectFullName().equals(prop2.getProjectFullName())) {
            return false;
        }
        if (!prop1.getProjectName().equals(prop2.getProjectName())) {
            return false;
        }
        return true;
    }

    public static NbGradleMultiProjectDef createEmpty(File projectDir) {
        NbGradleProjectTree emptyTree = NbGradleProjectTree.createEmpty(projectDir);
        return new NbGradleMultiProjectDef(emptyTree, emptyTree);
    }


    public NbGradleProjectTree getRootProject() {
        return rootProject;
    }

    public NbGradleProjectTree getMainProject() {
        return mainProject;
    }

    public File getProjectDir() {
        return mainProject.getProjectDir();
    }
}
