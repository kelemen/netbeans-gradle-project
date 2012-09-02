package org.netbeans.gradle.project.tasks;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.debugger.jpda.DebuggerStartException;
import org.netbeans.api.debugger.jpda.JPDADebugger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.query.GradleClassPathProvider;
import org.openide.filesystems.FileUtil;

public final class AttacherListener implements DebugTextListener.DebugeeListener {
    private static final Logger LOGGER = Logger.getLogger(AttacherListener.class.getName());

    private final NbGradleProject project;

    public AttacherListener(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    private ClassPath getClassPath(String type) {
        GradleClassPathProvider provider = project.getLookup().lookup(GradleClassPathProvider.class);
        return provider.getClassPaths(type);
    }

    private void doAttach(int port) throws DebuggerStartException {
        LOGGER.log(Level.INFO, "Attempting to attach to debugee on port: {0}", port);

        Map<String, Object> services = new HashMap<String, Object>();
        services.put("name", project.getAvailableModel().getMainModule().getUniqueName());
        services.put("baseDir", FileUtil.toFile(project.getProjectDirectory()));
        services.put("jdksources", getClassPath(ClassPath.BOOT));
        services.put("sourcepath", getClassPath(ClassPath.SOURCE));

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
