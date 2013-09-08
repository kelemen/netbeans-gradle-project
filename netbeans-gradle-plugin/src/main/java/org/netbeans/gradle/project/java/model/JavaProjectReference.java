package org.netbeans.gradle.project.java.model;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class JavaProjectReference {
    private static final Logger LOGGER = Logger.getLogger(JavaProjectReference.class.getName());

    private final File projectDir;
    private volatile NbJavaModule initialModule;

    private final AtomicReference<Project> projectRef;
    private final AtomicReference<JavaExtension> javaExtensionRef;

    public JavaProjectReference(File projectDir, NbJavaModule initialModule) {
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (initialModule == null) throw new NullPointerException("initialModule");

        this.projectDir = projectDir;
        this.initialModule = initialModule;

        this.projectRef = new AtomicReference<Project>(null);
        this.javaExtensionRef = new AtomicReference<JavaExtension>(null);
    }

    public File getProjectDir() {
        return projectDir;
    }

    public Project tryGetProject() {
        Project result = projectRef.get();
        if (result == null) {
            FileObject projectDirObj = FileUtil.toFileObject(projectDir);
            try {
                Closeable safeToOpen = NbGradleProjectFactory.safeToOpen(projectDir);
                try {
                    result = ProjectManager.getDefault().findProject(projectDirObj);
                } finally {
                    safeToOpen.close();
                }
            } catch (IOException ex) {
                LOGGER.log(Level.INFO, "Failed to open project.", ex);
            }

            if (result != null) {
                projectRef.set(result);
            }
            result = projectRef.get();
        }
        return result;
    }

    public JavaExtension tryGetJavaExtension() {
        JavaExtension result = javaExtensionRef.get();
        if (result == null) {
            Project project = tryGetProject();
            if (project != null) {
                result = project.getLookup().lookup(JavaExtension.class);
                if (result != null) {
                    javaExtensionRef.set(result);
                }
            }

            result = javaExtensionRef.get();
        }
        return result;
    }

    public NbJavaModule tryGetModule() {
        JavaExtension javaExt = tryGetJavaExtension();
        if (javaExt == null) {
            return null;
        }

        if (!javaExt.hasEverBeenLoaded()) {
            return initialModule;
        }

        // This reference is no longer needed, allow it to be garbage collected.
        initialModule = null;
        return javaExt.getCurrentModel().getMainModule();
    }
}
