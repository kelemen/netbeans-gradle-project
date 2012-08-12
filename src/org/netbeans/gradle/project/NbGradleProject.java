package org.netbeans.gradle.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;
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

    private final GradleClassPathProvider cpProvider;

    private final ChangeSupport modelChanges;
    private final AtomicBoolean hasModelBeenLoaded;
    private final AtomicReference<NbGradleModel> currentModelRef;

    public NbGradleProject(FileObject projectDir, ProjectState state) throws IOException {
        this.projectDir = projectDir;
        this.state = state;
        this.lookupRef = new AtomicReference<Lookup>(null);
        this.cpProvider = new GradleClassPathProvider(this);

        this.hasModelBeenLoaded = new AtomicBoolean(false);
        this.modelChanges = new ChangeSupport(this);
        this.currentModelRef = new AtomicReference<NbGradleModel>(GradleModelLoader.createEmptyModel(projectDir));
    }

    public void addModelChangeListener(ChangeListener listener) {
        modelChanges.addChangeListener(listener);
    }

    public void removeModelChangeListener(ChangeListener listener) {
        modelChanges.removeChangeListener(listener);
    }

    public NbGradleModel getCurrentModel() {
        loadProject(true, true);
        return currentModelRef.get();
    }

    public void reloadProject() {
        reloadProject(false);
    }

    private void reloadProject(boolean mayUseCache) {
        loadProject(false, mayUseCache);
    }

    private void loadProject(boolean onlyIfNotLoaded, boolean mayUseCache) {
        if (!hasModelBeenLoaded.compareAndSet(false, true)) {
            if (onlyIfNotLoaded) {
                return;
            }
        }

        GradleModelLoader.fetchModel(projectDir, mayUseCache, new ModelRetrievedListener() {
            @Override
            public void onComplete(NbGradleModel model, Throwable error) {
                boolean hasChanged = false;
                if (model != null) {
                    NbGradleModel lastModel = currentModelRef.getAndSet(model);
                    hasChanged = lastModel != model;
                }

                if (error != null) {
                    LOGGER.log(Level.WARNING, "Error while loading the project model.", error);
                }

                if (hasChanged) {
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            modelChanges.fireChange();
                        }
                    });
                }
            }
        });
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
                new GradleUnitTestFinder(this),
                new GradleSharabilityQuery(this),
                new GradleSourceForBinaryQuery(this),
                new GradleBinaryForSourceQuery(this),
                new OpenHook(),
            });

            if (lookupRef.compareAndSet(null, newLookup)) {
                // FIXME: There is a theoretical chance that these objects will
                //        not be notified of the change in the project model:
                //        Another concurrent getLookup() call may return these
                //        objects and may cause them to fetch the curren model.
                //        If it happens before registering and the model also
                //        changes before registering but after the lookup object
                //        retrieved the current model, it will miss this event.
                for (final ProjectChangeListener listener: newLookup.lookupAll(ProjectChangeListener.class)) {
                    // Note that there is no reason to unregister these
                    // listeners because we only register them once, so they
                    // cannot accumulate over time causing a memory leak.
                    modelChanges.addChangeListener(new ChangeListener() {
                        @Override
                        public void stateChanged(ChangeEvent e) {
                            listener.projectChanged();
                        }
                    });
                }
            }
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
            reloadProject(true);

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
