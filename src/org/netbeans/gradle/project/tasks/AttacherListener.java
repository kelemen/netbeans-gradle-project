package org.netbeans.gradle.project.tasks;

import java.net.URI;
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
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbDependency;
import org.netbeans.gradle.project.model.NbDependencyType;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModelUtils;
import org.netbeans.gradle.project.model.NbModuleDependency;
import org.netbeans.gradle.project.model.NbSourceGroup;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.gradle.project.model.NbUriDependency;
import org.netbeans.gradle.project.output.DebugTextListener;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class AttacherListener implements DebugTextListener.DebugeeListener {
    private static final Logger LOGGER = Logger.getLogger(AttacherListener.class.getName());

    private final NbGradleProject project;
    private final boolean test;

    public AttacherListener(NbGradleProject project, boolean test) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.test = test;
    }

    private ClassPath getSources() {
        NbGradleModule mainModule = project.getAvailableModel().getMainModule();
        List<FileObject> srcRoots = new LinkedList<FileObject>();

        srcRoots.addAll(mainModule.getSources(NbSourceType.SOURCE).getFileObjects());
        if (test) {
            srcRoots.addAll(mainModule.getSources(NbSourceType.TEST_SOURCE).getFileObjects());
        }

        Collection<NbDependency> allDependencies = NbModelUtils.getAllDependencies(
                mainModule,
                test ? NbDependencyType.TEST_RUNTIME : NbDependencyType.RUNTIME);

        for (NbDependency dependency: allDependencies) {
            if (dependency instanceof NbModuleDependency) {
                NbModuleDependency moduleDep = (NbModuleDependency)dependency;
                NbSourceGroup sources = moduleDep.getModule().getSources(NbSourceType.SOURCE);
                srcRoots.addAll(sources.getFileObjects());
            }
            else if (dependency instanceof NbUriDependency) {
                NbUriDependency uriDep = (NbUriDependency)dependency;
                URI srcUri = uriDep.getSrcUri();
                FileObject srcRoot = srcUri != null
                        ? NbModelUtils.uriToFileObject(srcUri) : null;
                srcRoot = GradleFileUtils.asArchiveOrDir(srcRoot);

                if (srcRoot != null) {
                    srcRoots.add(srcRoot);
                }
            }
        }
        return ClassPathSupport.createClassPath(srcRoots.toArray(new FileObject[0]));
    }

    private ClassPath getJdkSources() {
        JavaPlatform platform = project.getProperties().getPlatform().getValue();
        return platform.getSourceFolders();
    }

    private void doAttach(int port) throws DebuggerStartException {
        LOGGER.log(Level.INFO, "Attempting to attach to debugee on port: {0}", port);

        Map<String, Object> services = new HashMap<String, Object>();
        services.put("name", project.getAvailableModel().getMainModule().getUniqueName());
        services.put("baseDir", FileUtil.toFile(project.getProjectDirectory()));
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
