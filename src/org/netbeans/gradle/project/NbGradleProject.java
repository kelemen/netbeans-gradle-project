package org.netbeans.gradle.project;

import java.awt.EventQueue;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.tooling.BuildLauncher;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.CopyOperationImplementation;
import org.netbeans.spi.project.DeleteOperationImplementation;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.netbeans.spi.project.ui.support.DefaultProjectOperations;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Cancellable;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;

public final class NbGradleProject implements Project {
    private static final Logger LOGGER = Logger.getLogger(NbGradleProject.class.getName());

    public static final RequestProcessor PROJECT_PROCESSOR
            = new RequestProcessor("Gradle-Project-Processor", 1, true);

    public static final RequestProcessor TASK_EXECUTOR
            = new RequestProcessor("Gradle-Task-Executor", 1, true);

    private final FileObject projectDir;
    private final ProjectState state;
    private final AtomicReference<Lookup> lookupRef;

    private final ReentrantLock projectLoadLock;
    private volatile NbProjectModel gradleModel;

    private final ClassPathProvider cpProvider;

    public NbGradleProject(FileObject projectDir, ProjectState state) {
        this.projectDir = projectDir;
        this.state = state;
        this.lookupRef = new AtomicReference<Lookup>(null);
        this.projectLoadLock = new ReentrantLock();
        this.gradleModel = null;
        this.cpProvider = new GradleClassPathProvider(this);
    }

    public void reloadProject() {
        // FIXME: This is not entirely correct because at this point, the
        // loadProject() method might be just before the "gradleModel = cached"
        // statement and in this case we will not reload as requested.
        gradleModel = null;
    }

    private NbProjectModel parseProject() {
        LOGGER.log(Level.INFO, "Loading Gradle project from directory: {0}", getProjectDirectory());

        // TODO: show progress ...

        GradleConnector gradleConnector = GradleConnector.newConnector();
        gradleConnector.forProjectDirectory(FileUtil.toFile(getProjectDirectory()));
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            final EclipseProject eclipseModel
                    = projectConnection.getModel(EclipseProject.class);
            final IdeaProject ideaModel
                    = projectConnection.getModel(IdeaProject.class);
            return new NbProjectModel() {
                @Override
                public EclipseProject getEclipseModel() {
                    return eclipseModel;
                }

                @Override
                public IdeaProject getIdeaModel() {
                    return ideaModel;
                }
            };
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }
    }

    public NbProjectModel loadProject() {
        NbProjectModel cached = gradleModel;
        if (cached == null) {
            if (projectLoadLock.isHeldByCurrentThread()) {
                throw new IllegalStateException("Recursive project loading.");
            }

            projectLoadLock.lock();
            try {
                cached = gradleModel;
                if (cached == null) {
                    cached = parseProject();
                    gradleModel = cached;
                }
            } finally {
                projectLoadLock.unlock();
            }
        }
        return cached;
    }

    public NbProjectModel tryGetCachedProject() {
        return gradleModel;
    }

    public String getName() {
        return getProjectDirectory().getName();
    }

    public String getDisplayName() {
        return getProjectDirectory().getName();
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    @Override
    public Lookup getLookup() {
        // The Lookup is not created in the constructor, so that we do not need
        // to share "this" in the constructor.
        Lookup result = lookupRef.get();
        if (result == null) {
            Lookup newLookup = Lookups.fixed(new Object[] {
                this,
                state, //allow outside code to mark the project as needing saving
                new GradleProjectInformation(this),
                new GradleProjectLogicalViewProvider(this),
                new GradleProjectSources(this),
                cpProvider,
                new GradleSourceLevelQueryImplementation(this),
                //new OpenHook(),
            });
            lookupRef.compareAndSet(null, newLookup);
            result = lookupRef.get();
        }
        return result;
    }

    private class OpenHook extends ProjectOpenedHook {
        private ClassPath[] sourcePaths;
        private ClassPath[] compilePaths;
        private ClassPath[] bootPaths;

        @Override
        protected void projectOpened() {
            sourcePaths = new ClassPath[] {cpProvider.findClassPath(null, ClassPath.SOURCE)};
            compilePaths = new ClassPath[] {cpProvider.findClassPath(null, ClassPath.COMPILE)};
            bootPaths = new ClassPath[] {cpProvider.findClassPath(null, ClassPath.BOOT)};

            GlobalPathRegistry.getDefault().register(ClassPath.BOOT, bootPaths);
            GlobalPathRegistry.getDefault().register(ClassPath.COMPILE, compilePaths);
            GlobalPathRegistry.getDefault().register(ClassPath.SOURCE, sourcePaths);
        }

        @Override
        protected void projectClosed() {
            GlobalPathRegistry.getDefault().unregister(ClassPath.BOOT, bootPaths);
            GlobalPathRegistry.getDefault().unregister(ClassPath.COMPILE, compilePaths);
            GlobalPathRegistry.getDefault().unregister(ClassPath.SOURCE, sourcePaths);
        }
    }

    private final class GradleActionProvider implements ActionProvider {

        private String[] supported = new String[]{
            ActionProvider.COMMAND_RUN,
            ActionProvider.COMMAND_DELETE,
            ActionProvider.COMMAND_COPY,};

        @Override
        public String[] getSupportedActions() {
            return supported;
        }

        @Override
        public void invokeAction(String string, Lookup lookup) throws IllegalArgumentException {
            if (string.equalsIgnoreCase(ActionProvider.COMMAND_DELETE)) {
                DefaultProjectOperations.performDefaultDeleteOperation(NbGradleProject.this);
            }
            if (string.equalsIgnoreCase(ActionProvider.COMMAND_RUN)) {
                EventQueue.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        ProgressHandle h = ProgressHandleFactory.createHandle("Running...", new Cancellable() {
                            @Override
                            public boolean cancel() {
                                return true;
                            }
                        });
                        h.start();
                        GradleConnector gradleConnector = GradleConnector.newConnector();
                        gradleConnector.forProjectDirectory(FileUtil.toFile(getProjectDirectory()));
                        ProjectConnection projectConnection = gradleConnector.connect();
                        BuildLauncher buildLauncher = projectConnection.newBuild();
                        buildLauncher.forTasks("run");
                        buildLauncher.run();
                        h.finish();
                    }
                });
            }
            if (string.equalsIgnoreCase(ActionProvider.COMMAND_COPY)) {
                DefaultProjectOperations.performDefaultCopyOperation(NbGradleProject.this);
            }
        }

        @Override
        public boolean isActionEnabled(String command, Lookup lookup) throws IllegalArgumentException {
            if ((command.equals(ActionProvider.COMMAND_DELETE))) {
                return true;
            } else if ((command.equals(ActionProvider.COMMAND_COPY))) {
                return true;
            } else if ((command.equals(ActionProvider.COMMAND_RUN))) {
                return true;
            } else {
                throw new IllegalArgumentException(command);
            }
        }
    }

    private final class GradleProjectDeleteOperation implements DeleteOperationImplementation {

        @Override
        public void notifyDeleting() throws IOException {
        }

        @Override
        public void notifyDeleted() throws IOException {
        }

        @Override
        public List<FileObject> getMetadataFiles() {
            List<FileObject> dataFiles = new ArrayList<FileObject>();
            return dataFiles;
        }

        @Override
        public List<FileObject> getDataFiles() {
            List<FileObject> dataFiles = new ArrayList<FileObject>();
            return dataFiles;
        }
    }

    private final class GradleProjectCopyOperation implements CopyOperationImplementation {

        private final NbGradleProject project;
        private final FileObject projectDir;

        public GradleProjectCopyOperation(NbGradleProject project) {
            this.project = project;
            this.projectDir = project.getProjectDirectory();
        }

        @Override
        public List<FileObject> getMetadataFiles() {
            return Collections.emptyList();
        }

        @Override
        public List<FileObject> getDataFiles() {
            return Collections.emptyList();
        }

        @Override
        public void notifyCopying() throws IOException {
        }

        @Override
        public void notifyCopied(Project arg0, File arg1, String arg2) throws IOException {
        }
    }
}
