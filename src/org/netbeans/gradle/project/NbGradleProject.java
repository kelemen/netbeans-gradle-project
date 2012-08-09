package org.netbeans.gradle.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.Model;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
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

    private final GradleClassPathProvider cpProvider;

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

    private static <T extends Model> T getModelWithProgress(
            final ProgressHandle progress,
            ProjectConnection projectConnection,
            Class<T> model) {
        ModelBuilder<T> builder = projectConnection.model(model);
        builder.addProgressListener(new ProgressListener() {
            @Override
            public void statusChanged(ProgressEvent pe) {
                progress.progress(pe.getDescription());
            }
        });

        return builder.get();
    }

    private NbProjectModel parseProjectWitProgress(ProgressHandle progress) {
        GradleConnector gradleConnector = GradleConnector.newConnector();
        gradleConnector.forProjectDirectory(FileUtil.toFile(getProjectDirectory()));
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            final EclipseProject eclipseModel
                    = getModelWithProgress(progress, projectConnection, EclipseProject.class);
            final IdeaProject ideaModel
                    = getModelWithProgress(progress, projectConnection, IdeaProject.class);
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

    private NbProjectModel parseProject() {
        LOGGER.log(Level.INFO, "Loading Gradle project from directory: {0}", projectDir);

        ProgressHandle progress = ProgressHandleFactory.createHandle(
                NbBundle.getMessage(NbGradleProject.class, "LBL_LoadingProject", projectDir.getNameExt()));
        try {
            progress.start();
            return parseProjectWitProgress(progress);
        } finally {
            progress.finish();
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
                new GradleActionProvider(this),
                cpProvider,
                new GradleSourceLevelQueryImplementation(this),
                new NbUnitTestFinder(this),
                new GradleSharabilityQuery(this),
                new GradleSourceForBinaryQuery(this),
                new GradleBinaryForSourceQuery(this),
                new OpenHook(),
            });
            lookupRef.compareAndSet(null, newLookup);
            result = lookupRef.get();
        }
        return result;
    }

    // equals and hashCode is provided, so that NetBeans doesn't load the
    // same project multiple times.

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 67 * hash + (this.projectDir != null ? this.projectDir.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final NbGradleProject other = (NbGradleProject)obj;
        if (this.projectDir != other.projectDir && (this.projectDir == null || !this.projectDir.equals(other.projectDir)))
            return false;
        return true;
    }

    // OpenHook is important for debugging because the debugger relies on the
    // globally registered source class paths for source stepping.

    // SwingUtilities.invokeLater is used only to guarantee the order of events.
    // Actually any executor which executes tasks in the order they were
    // submitted to it is good (using SwingUtilities.invokeLater was only
    // convenient to use because registering paths is cheap enough).
    private class OpenHook extends ProjectOpenedHook implements PropertyChangeListener {
        private boolean opened;
        private ClassPath[] sourcePaths;
        private ClassPath[] compilePaths;

        public OpenHook() {
            this.opened = false;
            this.sourcePaths = null;
            this.compilePaths = null;
        }

        @Override
        protected void projectOpened() {
            cpProvider.addPropertyChangeListener(this);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    opened = true;
                    doRegisterClassPaths();
                }
            });
        }

        @Override
        protected void projectClosed() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    opened = false;
                    doUnregisterPaths();
                }
            });

            cpProvider.removePropertyChangeListener(this);
        }

        private void doUnregisterPaths() {
            assert SwingUtilities.isEventDispatchThread();

            GlobalPathRegistry registry = GlobalPathRegistry.getDefault();
            if (compilePaths != null) {
                registry.unregister(ClassPath.COMPILE, compilePaths);
                compilePaths = null;
            }
            if (sourcePaths != null) {
                registry.unregister(ClassPath.SOURCE, sourcePaths);
                sourcePaths = null;
            }
        }

        private void doRegisterClassPaths() {
            assert SwingUtilities.isEventDispatchThread();

            doUnregisterPaths();

            GlobalPathRegistry registry = GlobalPathRegistry.getDefault();

            compilePaths = new ClassPath[]{cpProvider.getCompilePaths()};
            registry.register(ClassPath.COMPILE, compilePaths);

            sourcePaths = new ClassPath[]{cpProvider.getSourcePaths()};
            registry.register(ClassPath.SOURCE, sourcePaths);
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (opened) {
                        doRegisterClassPaths();
                    }
                }
            });
        }
    }
}
