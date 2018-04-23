package org.netbeans.gradle.project.tasks;

import java.beans.PropertyChangeEvent;
import java.io.File;
import java.net.URL;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.cancel.CancellationController;
import org.netbeans.api.debugger.jpda.DebuggerStartException;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.JavaProjectDependencies;
import org.netbeans.gradle.project.java.model.JavaProjectDependencyDef;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.util.DefaultUrlFactory;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.util.UrlFactory;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;

public final class AttacherListener implements DebugTextListener.DebugeeListener {
    private static final Logger LOGGER = Logger.getLogger(AttacherListener.class.getName());

    private final JavaExtension javaExt;
    private final CancellationController buildCancel;

    public AttacherListener(JavaExtension javaExt, CancellationController buildCancel) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.buildCancel = Objects.requireNonNull(buildCancel, "buildCancel");
    }

    private static void addSourcesOfModule(NbJavaModule module, Set<FileObject> result) {
        for (JavaSourceSet sourceSet: module.getSources()) {
            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                for (File root: sourceGroup.getSourceRoots()) {
                    FileObject rootObj = NbFileUtils.asArchiveOrDir(root);
                    if (rootObj != null) {
                        result.add(rootObj);
                    }
                }
            }
        }
    }

    private static void getBinaryRuntimeDependencies(NbJavaModule module, Set<File> binaries) {
        for (JavaSourceSet sourceSet: module.getSources()) {
            Set<File> runtimeClasspath = sourceSet.getClasspaths().getRuntimeClasspaths();
            binaries.addAll(runtimeClasspath);
        }
    }

    private static void addSourcesOfBinaries(Collection<File> binaries, Set<FileObject> result) {
        UrlFactory urlFactory = DefaultUrlFactory.getDefaultArchiveOrDirFactory();
        for (File binary: binaries) {
            URL url = urlFactory.toUrl(binary);
            if (url == null) continue;

            SourceForBinaryQuery.Result2 sourceResult = SourceForBinaryQuery.findSourceRoots2(url);
            if (sourceResult == null) continue;

            FileObject[] roots = sourceResult.getRoots();
            if (roots == null) continue;

            result.addAll(Arrays.asList(roots));
        }
    }

    private static ClassPath getSources(JavaExtension javaExt) {
        NbJavaModel currentModel = javaExt.getCurrentModel();

        NbJavaModule mainModule = currentModel.getMainModule();
        Set<FileObject> srcRoots = new LinkedHashSet<>();
        Set<File> runtimeDependencies = new HashSet<>(100);

        addSourcesOfModule(mainModule, srcRoots);
        getBinaryRuntimeDependencies(mainModule, runtimeDependencies);

        JavaProjectDependencies projectDependencies = javaExt.getProjectDependencies();
        for (JavaProjectDependencyDef dependency: projectDependencies.translatedDependencies().getValue().values()) {
            NbJavaModule module = dependency.getJavaModule();
            addSourcesOfModule(module, srcRoots);
            getBinaryRuntimeDependencies(module, runtimeDependencies);
        }

        addSourcesOfBinaries(runtimeDependencies, srcRoots);

        return ClassPathSupport.createClassPath(srcRoots.toArray(new FileObject[0]));
    }

    private static ClassPath getJdkSources(JavaExtension javaExt) {
        GradleProperty.BuildPlatform platformProperty
                = javaExt.getOwnerProjectLookup().lookup(GradleProperty.BuildPlatform.class);
        ProjectPlatform platform = platformProperty.getValue();
        return ClassPathSupport.createClassPath(platform.getSourcePaths().toArray(new URL[0]));
    }

    public static Map<String, Object> getJpdaServiceObjects(JavaExtension javaExt) {
        Map<String, Object> result = new HashMap<>();
        result.put("name", javaExt.getCurrentModel().getMainModule().getUniqueName());
        result.put("baseDir", javaExt.getProjectDirectoryAsFile());
        result.put("jdksources", getJdkSources(javaExt));
        result.put("sourcepath", getSources(javaExt));
        return result;
    }

    private void doAttach(int port) throws DebuggerStartException {
        LOGGER.log(Level.INFO, "Attempting to attach to debugee on port: {0}", port);

        Map<String, Object> services = getJpdaServiceObjects(javaExt);

        final JPDADebugger debugger = JPDADebugger.attach("127.0.0.1", port, new Object[]{services});
        debugger.addPropertyChangeListener("state", (PropertyChangeEvent evt) -> {
            if (debugger.getState() == JPDADebugger.STATE_DISCONNECTED) {
                buildCancel.cancel();
            }
        });
    }

    @Override
    public void onDebugeeListening(int port) {
        NbTaskExecutors.DEFAULT_EXECUTOR.execute(() -> {
            try {
                doAttach(port);
            } catch (DebuggerStartException ex) {
                LOGGER.log(Level.INFO, "Failed to attach to debugee.", ex);
            }
        });
    }
}
