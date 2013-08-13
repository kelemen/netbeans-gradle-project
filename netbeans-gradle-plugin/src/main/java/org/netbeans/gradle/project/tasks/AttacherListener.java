package org.netbeans.gradle.project.tasks;

import java.net.URI;
import java.net.URL;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.debugger.jpda.DebuggerStartException;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.NbDependencyType;
import org.netbeans.gradle.project.java.model.NbJavaDependency;
import org.netbeans.gradle.project.java.model.NbJavaModelUtils;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbModuleDependency;
import org.netbeans.gradle.project.java.model.NbSourceGroup;
import org.netbeans.gradle.project.java.model.NbSourceType;
import org.netbeans.gradle.project.java.model.NbUriDependency;
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

    private ClassPath getSources() {
        NbJavaModule mainModule = javaExt.getCurrentModel().getMainModule();
        List<FileObject> srcRoots = new LinkedList<FileObject>();

        srcRoots.addAll(mainModule.getSources(NbSourceType.SOURCE).getFileObjects());
        if (test) {
            srcRoots.addAll(mainModule.getSources(NbSourceType.TEST_SOURCE).getFileObjects());
        }

        Collection<NbJavaDependency> allDependencies = NbJavaModelUtils.getAllDependencies(
                mainModule,
                test ? NbDependencyType.TEST_RUNTIME : NbDependencyType.RUNTIME);

        for (NbJavaDependency dependency: allDependencies) {
            if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                NbSourceGroup sources = moduleDep.getModule().getSources(NbSourceType.SOURCE);
                srcRoots.addAll(sources.getFileObjects());
            }
            else if (dependency instanceof NbUriDependency) {
                NbUriDependency uriDep = (NbUriDependency)dependency;
                URI srcUri = uriDep.getSrcUri();
                FileObject srcRoot = srcUri != null
                        ? NbJavaModelUtils.uriToFileObject(srcUri) : null;
                srcRoot = GradleFileUtils.asArchiveOrDir(srcRoot);

                if (srcRoot != null) {
                    srcRoots.add(srcRoot);
                }
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
