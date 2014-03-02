package org.netbeans.gradle.project.java.model;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.java.JavaExtension;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class JavaProjectReference implements Serializable {
    private static final long serialVersionUID = 1L;

    private static final Logger LOGGER = Logger.getLogger(JavaProjectReference.class.getName());

    private final File projectDir;
    private volatile NbJavaModule initialModule;

    private final AtomicReference<Project> projectRef;
    private final AtomicReference<JavaExtension> javaExtensionRef;

    public JavaProjectReference(File projectDir, NbJavaModule initialModule) {
        ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
        ExceptionHelper.checkNotNullArgument(initialModule, "initialModule");

        this.projectDir = projectDir;
        this.initialModule = initialModule;

        this.projectRef = new AtomicReference<>(null);
        this.javaExtensionRef = new AtomicReference<>(null);
    }

    public File getProjectDir() {
        return projectDir;
    }

    public Project tryGetProject() {
        Project result = projectRef.get();
        if (result == null) {
            FileObject projectDirObj = FileUtil.toFileObject(projectDir);
            try {
                try (Closeable safeToOpen = NbGradleProjectFactory.safeToOpen(projectDir)) {
                    assert safeToOpen != null; // Avoid warning

                    result = ProjectManager.getDefault().findProject(projectDirObj);
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
            NbJavaModule result = initialModule;
            if (result != null) {
                return result;
            }
            // Else: This means that some other thread noticed that
            //   javaExt has been loaded and set initialModule to null.
        }

        // This reference is no longer needed, allow it to be garbage collected.
        initialModule = null;
        return javaExt.getCurrentModel().getMainModule();
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final File projectDir;
        private final NbJavaModule initialModule;

        public SerializedFormat(JavaProjectReference source) {
            this.projectDir = source.projectDir;
            this.initialModule = source.tryGetModule();
        }

        private Object readResolve() throws ObjectStreamException {
            return new JavaProjectReference(projectDir, initialModule);
        }
    }
}
