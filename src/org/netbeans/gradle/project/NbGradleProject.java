package org.netbeans.gradle.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.model.ModelLoadListener;
import org.netbeans.gradle.project.model.ModelRetrievedListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.GradleCustomizer;
import org.netbeans.gradle.project.properties.NbGradleConfigProvider;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.gradle.project.properties.ProjectPropertiesManager;
import org.netbeans.gradle.project.properties.ProjectPropertiesProxy;
import org.netbeans.gradle.project.properties.PropertiesLoadListener;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.query.GradleAnnotationProcessingQuery;
import org.netbeans.gradle.project.query.GradleBinaryForSourceQuery;
import org.netbeans.gradle.project.query.GradleCacheBinaryForSourceQuery;
import org.netbeans.gradle.project.query.GradleCacheSourceForBinaryQuery;
import org.netbeans.gradle.project.query.GradleClassPathProvider;
import org.netbeans.gradle.project.query.GradleSharabilityQuery;
import org.netbeans.gradle.project.query.GradleSourceEncodingQuery;
import org.netbeans.gradle.project.query.GradleSourceForBinaryQuery;
import org.netbeans.gradle.project.query.GradleSourceLevelQueryImplementation;
import org.netbeans.gradle.project.query.GradleUnitTestFinder;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.gradle.project.view.GradleProjectLogicalViewProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class NbGradleProject implements Project {
    private static final Logger LOGGER = Logger.getLogger(NbGradleProject.class.getName());

    public static final RequestProcessor PROJECT_PROCESSOR
            = new RequestProcessor("Gradle-Project-Processor", 1, true);

    private final FileObject projectDir;
    private final ProjectState state;
    private final AtomicReference<Lookup> lookupRef;

    private final GradleClassPathProvider cpProvider;

    private final String name;
    private final ExceptionDisplayer exceptionDisplayer;
    private final ChangeSupport modelChanges;
    private final AtomicBoolean hasModelBeenLoaded;
    private final AtomicReference<NbGradleModel> currentModelRef;
    private final ProjectPropertiesProxy properties;
    private final ProjectInfoManager projectInfoManager;

    private final AtomicReference<ProjectInfoRef> loadErrorRef;

    private final WaitableSignal loadedAtLeastOnceSignal;

    public NbGradleProject(FileObject projectDir, ProjectState state) throws IOException {
        this.projectDir = projectDir;
        this.state = state;
        this.lookupRef = new AtomicReference<Lookup>(null);
        this.properties = new ProjectPropertiesProxy(this);
        this.projectInfoManager = new ProjectInfoManager();

        this.hasModelBeenLoaded = new AtomicBoolean(false);
        this.loadErrorRef = new AtomicReference<ProjectInfoRef>(null);
        this.modelChanges = new ChangeSupport(this);
        this.currentModelRef = new AtomicReference<NbGradleModel>(GradleModelLoader.createEmptyModel(projectDir));

        this.cpProvider = new GradleClassPathProvider(this);
        this.loadedAtLeastOnceSignal = new WaitableSignal();
        this.name = projectDir.getNameExt();
        this.exceptionDisplayer = new ExceptionDisplayer(NbStrings.getProjectErrorTitle(name));
    }

    public NbGradleConfiguration getCurrentProfile() {
        return getLookup().lookup(NbGradleConfigProvider.class).getActiveConfiguration();
    }

    public void addProfileChangeListener(ChangeListener listener) {
        getLookup().lookup(NbGradleConfigProvider.class).addActiveConfigChangeListener(listener);
    }

    public void removeProfileChangeListener(ChangeListener listener) {
        getLookup().lookup(NbGradleConfigProvider.class).removeActiveConfigChangeListener(listener);
    }

    public void displayError(String errorText, Throwable exception, boolean setFocus) {
        exceptionDisplayer.displayError(errorText, exception, setFocus);
    }

    private ProjectInfoRef getLoadErrorRef() {
        ProjectInfoRef result = loadErrorRef.get();
        if (result == null) {
            loadErrorRef.compareAndSet(null, getProjectInfoManager().createInfoRef());
            result = loadErrorRef.get();
        }
        return result;
    }

    public ProjectInfoManager getProjectInfoManager() {
        return projectInfoManager;
    }

    public void addModelChangeListener(ChangeListener listener) {
        modelChanges.addChangeListener(listener);
    }

    public void removeModelChangeListener(ChangeListener listener) {
        modelChanges.removeChangeListener(listener);
    }

    public NbGradleModel getAvailableModel() {
        return currentModelRef.get();
    }

    public NbGradleModel getCurrentModel() {
        loadProject(true, true);
        return getAvailableModel();
    }

    public void reloadProject() {
        reloadProject(false);
    }

    private void reloadProject(boolean mayUseCache) {
        loadProject(false, mayUseCache);
    }

    public boolean hasLoadedProject() {
        return loadedAtLeastOnceSignal.isSignaled();
    }

    public boolean tryWaitForLoadedProject() {
        if (GradleDaemonManager.isRunningExclusiveTask()) {
            throw new IllegalStateException("Cannot wait for loading a project"
                    + " while blocking daemon tasks from being executed."
                    + " Possible dead-lock.");
        }

        // Ensure that the project is started to be loaded.
        getCurrentModel();
        return loadedAtLeastOnceSignal.tryWaitForSignal();
    }

    private void onModelChange() {
        assert SwingUtilities.isEventDispatchThread();

        try {
            modelChanges.fireChange();
        } finally {
            GradleCacheSourceForBinaryQuery.notifyCacheChange();
            GradleCacheBinaryForSourceQuery.notifyCacheChange();
        }
    }

    private void loadProject(boolean onlyIfNotLoaded, boolean mayUseCache) {
        if (!hasModelBeenLoaded.compareAndSet(false, true)) {
            if (onlyIfNotLoaded) {
                return;
            }
        }

        GradleModelLoader.fetchModel(projectDir, mayUseCache, new ModelRetrievedListenerImpl());
    }

    public ProjectProperties getProperties() {
        return properties;
    }

    public ProjectProperties getPropertiesForProfile(
            String profile,
            boolean useInheritance,
            PropertiesLoadListener onLoadTask) {

        File[] files = SettingsFiles.getFilesForProfile(this, profile);
        return useInheritance ?
                ProjectPropertiesManager.getProperties(files, onLoadTask)
                : ProjectPropertiesManager.getProperties(files[0], onLoadTask);
    }

    public ProjectProperties tryGetLoadedProperties() {
        if (properties.tryWaitForLoaded()) {
            return properties;
        }
        else {
            return null;
        }
    }

    public String getName() {
        return name;
    }

    public String getDisplayName() {
        return name;
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
                NbGradleConfigProvider.getConfigProvider(this),
                new GradleProjectInformation(this),
                new GradleProjectLogicalViewProvider(this),
                new GradleProjectSources(this),
                new GradleActionProvider(this),
                cpProvider,
                new GradleSourceLevelQueryImplementation(this),
                new GradleUnitTestFinder(this),
                new GradleSharabilityQuery(this),
                new GradleSourceEncodingQuery(this),
                new GradleCustomizer(this),
                new OpenHook(),
                new GradleAnnotationProcessingQuery(),
                new GradleSourceForBinaryQuery(this),
                new GradleBinaryForSourceQuery(this),

                // FileOwnerQueryImplementation cannot be added to the project's
                // lookup, since NetBeans will ignore it. It must be added
                // using the ServiceProviders annotation. Our implementation is
                // GradleFileOwnerQuery and is added using the annotation.
            });

            if (lookupRef.compareAndSet(null, newLookup)) {
                for (ProjectInitListener listener: newLookup.lookupAll(ProjectInitListener.class)) {
                    listener.onInitProject();
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
        private final List<GlobalPathReg> paths;
        private final ModelLoadListener modelLoadListener;
        private boolean opened;

        public OpenHook() {
            this.opened = false;

            this.paths = new LinkedList<GlobalPathReg>();
            this.paths.add(new GlobalPathReg(ClassPath.SOURCE));
            this.paths.add(new GlobalPathReg(ClassPath.BOOT));
            this.paths.add(new GlobalPathReg(ClassPath.COMPILE));
            this.paths.add(new GlobalPathReg(ClassPath.EXECUTE));

            this.modelLoadListener = new ModelLoadListener() {
                @Override
                public void modelLoaded(NbGradleModel model) {
                    if (getProjectDirectory().equals(model.getProjectDir())) {
                        new ModelRetrievedListenerImpl().onComplete(model, null);
                    }
                }
            };
        }

        @Override
        protected void projectOpened() {
            GradleModelLoader.addModelLoadedListener(modelLoadListener);
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

            GradleModelLoader.removeModelLoadedListener(modelLoadListener);
            cpProvider.removePropertyChangeListener(this);
        }

        private void doUnregisterPaths() {
            assert SwingUtilities.isEventDispatchThread();

            for (GlobalPathReg pathReg: paths) {
                pathReg.unregister();
            }
        }

        private void doRegisterClassPaths() {
            assert SwingUtilities.isEventDispatchThread();

            for (GlobalPathReg pathReg: paths) {
                pathReg.register();
            }
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

    private class GlobalPathReg {
        private final String type;
        // Note that using AtomicReference does not really make the methods
        // thread-safe but is only convenient to use.
        private final AtomicReference<ClassPath[]> paths;

        public GlobalPathReg(String type) {
            this.type = type;
            this.paths = new AtomicReference<ClassPath[]>(null);
        }

        private void replaceRegistration(ClassPath[] newPaths) {
            GlobalPathRegistry registry = GlobalPathRegistry.getDefault();

            ClassPath[] oldPaths = paths.getAndSet(newPaths);
            if (oldPaths != null) {
                registry.unregister(type, oldPaths);
            }
            if (newPaths != null) {
                registry.register(type, newPaths);
            }
        }

        public void register() {
            ClassPath[] newPaths = new ClassPath[]{cpProvider.getClassPaths(type)};
            replaceRegistration(newPaths);
        }

        public void unregister() {
            replaceRegistration(null);
        }
    }

    private class ModelRetrievedListenerImpl implements ModelRetrievedListener {
        @Override
        public void onComplete(NbGradleModel model, Throwable error) {
            loadedAtLeastOnceSignal.signal();

            boolean hasChanged = false;
            if (model != null) {
                NbGradleModel lastModel = currentModelRef.getAndSet(model);
                hasChanged = lastModel != model;
            }

            if (error != null) {
                ProjectInfo.Entry entry = new ProjectInfo.Entry(
                        ProjectInfo.Kind.ERROR,
                        NbStrings.getErrorLoadingProject(error));
                getLoadErrorRef().setInfo(new ProjectInfo(Collections.singleton(entry)));
                LOGGER.log(Level.INFO, "Error while loading the project model.", error);
                displayError(NbStrings.getProjectLoadFailure(name), error, true);
            }
            else {
                getLoadErrorRef().setInfo(null);
            }

            if (hasChanged) {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        onModelChange();
                    }
                });
            }
        }
    }

    private static class ExceptionDisplayer {
        private final String outputCaption;
        private final Lock outputLock;

        public ExceptionDisplayer(String outputCaption) {
            if (outputCaption == null) throw new NullPointerException("outputCaption");
            this.outputCaption = outputCaption;
            this.outputLock = new ReentrantLock();
        }

        public void displayError(final String errorText, final Throwable exception, final boolean setFocus) {
            if (errorText == null) throw new NullPointerException("errorText");

            PROJECT_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    InputOutput io = IOProvider.getDefault().getIO(outputCaption, false);
                    if (setFocus) {
                        io.select();
                    }

                    OutputWriter err = io.getErr();
                    outputLock.lock();
                    try {
                        err.println();
                        err.println(errorText);
                        if (err != null) {
                            exception.printStackTrace(err);
                        }
                    } finally {
                        outputLock.unlock();
                        err.close();
                    }
                }
            });
        }
    }
}
