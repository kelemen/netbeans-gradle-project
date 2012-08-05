package org.netbeans.gradle.project;

import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
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
import org.netbeans.spi.java.classpath.ClassPathProvider;
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
                cpProvider,
                new GradleSourceLevelQueryImplementation(this),
                new NbUnitTestFinder(this),
                new GradleSharabilityQuery(this),
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

        // TODO: the current implementation is wrong

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
}
