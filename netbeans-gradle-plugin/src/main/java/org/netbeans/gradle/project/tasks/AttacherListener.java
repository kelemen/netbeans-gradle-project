package org.netbeans.gradle.project.tasks;

import java.io.File;
import java.net.URL;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.debugger.jpda.DebuggerStartException;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.JavaProjectReference;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;

public final class AttacherListener implements DebugTextListener.DebugeeListener {
    private static final Logger LOGGER = Logger.getLogger(AttacherListener.class.getName());

    private final JavaExtension javaExt;
    private final boolean test;

    public AttacherListener(JavaExtension javaExt, boolean test) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
        this.test = test;
    }

    private static void addSourcesOfModule(NbJavaModule module, Set<FileObject> result) {
        for (JavaSourceSet sourceSet: module.getSources()) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                for (File root: sourceGroup.getSourceRoots()) {
                    FileObject rootObj = GradleFileUtils.asArchiveOrDir(root);
                    if (rootObj != null) {
                        result.add(rootObj);
                    }
                }
            }
        }

        for (File root: module.getRelatedSources().getAllSourcesFiles()) {
            FileObject srcRootObj = GradleFileUtils.asArchiveOrDir(root);
            if (srcRootObj != null) {
                result.add(srcRootObj);
            }
        }
    }

    private ClassPath getSources() {
        NbJavaModel currentModel = javaExt.getCurrentModel();

        NbJavaModule mainModule = currentModel.getMainModule();
        Set<FileObject> srcRoots = new LinkedHashSet<FileObject>();

        addSourcesOfModule(mainModule, srcRoots);
        for (JavaProjectReference projectRef: currentModel.getAllDependencies()) {
            NbJavaModule module = projectRef.tryGetModule();
            if (module != null) {
                addSourcesOfModule(module, srcRoots);
            }
        }

        return ClassPathSupport.createClassPath(srcRoots.toArray(new FileObject[0]));
    }

    private ClassPath getJdkSources() {
        GradleProperty.BuildPlatform platformProperty
                = javaExt.getProjectLookup().lookup(GradleProperty.BuildPlatform.class);
        ProjectPlatform platform = platformProperty.getValue();
        return ClassPathSupport.createClassPath(platform.getSourcePaths().toArray(new URL[0]));
    }

    private void doAttach(int port) throws DebuggerStartException {
        LOGGER.log(Level.INFO, "Attempting to attach to debugee on port: {0}", port);

        Map<String, Object> services = new HashMap<String, Object>();
        services.put("name", javaExt.getCurrentModel().getMainModule().getUniqueName());
        services.put("baseDir", javaExt.getProjectDirectoryAsFile());
        services.put("jdksources", getJdkSources());
        services.put("sourcepath", getSources());

        JPDADebugger.attach("127.0.0.1", port, new Object[]{services});
    }

    @Override
    public void onDebugeeListening(final int port) {
        NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    doAttach(port);
                } catch (DebuggerStartException ex) {
                    LOGGER.log(Level.INFO, "Failed to attach to debugee.", ex);
                }
            }
        });
    }
}
