package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionQuery;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.GradleTaskVariableQuery;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.model.ModelLoadListener;
import org.netbeans.gradle.project.model.ModelRetrievedListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.GradleAuxiliaryConfiguration;
import org.netbeans.gradle.project.properties.GradleAuxiliaryProperties;
import org.netbeans.gradle.project.properties.GradleCustomizer;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.NbGradleSingleProjectConfigProvider;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.gradle.project.properties.ProjectPropertiesApi;
import org.netbeans.gradle.project.properties.ProjectPropertiesManager;
import org.netbeans.gradle.project.properties.ProjectPropertiesProxy;
import org.netbeans.gradle.project.properties.PropertiesLoadListener;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.query.GradleCacheBinaryForSourceQuery;
import org.netbeans.gradle.project.query.GradleCacheSourceForBinaryQuery;
import org.netbeans.gradle.project.query.GradleSharabilityQuery;
import org.netbeans.gradle.project.query.GradleSourceEncodingQuery;
import org.netbeans.gradle.project.query.GradleTemplateAttrProvider;
import org.netbeans.gradle.project.tasks.DefaultGradleCommandExecutor;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.netbeans.gradle.project.tasks.MergedBuiltInGradleCommandQuery;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.gradle.project.view.GradleProjectLogicalViewProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputWriter;

public final class NbGradleProject implements Project {
    private static final Logger LOGGER = Logger.getLogger(NbGradleProject.class.getName());

    // Note: There is a lot of assumption on that this executor is
    //   single-threaded and executes task in the order they were submitted.
    public static final RequestProcessor PROJECT_PROCESSOR
            = new RequestProcessor("Gradle-Project-Processor", 1, true);
    private static final LicenseManager LICENSE_MANAGER = new LicenseManager();

    private final FileObject projectDir;
    private final File projectDirAsFile;
    private final ProjectState state;
    private final AtomicReference<Lookup> defaultLookupRef;
    private final AtomicReference<DynamicLookup> lookupRef;
    private final AtomicReference<Lookup> protectedLookupRef;

    private final String name;
    private final ExceptionDisplayer exceptionDisplayer;
    private final ChangeSupport modelChanges;
    private final AtomicBoolean hasModelBeenLoaded;
    private final AtomicReference<NbGradleModelRef> currentModelRef;
    private final ProjectPropertiesProxy properties;
    private final ProjectInfoManager projectInfoManager;

    private final AtomicReference<ProjectInfoRef> loadErrorRef;

    private final WaitableSignal loadedAtLeastOnceSignal;

    private final AtomicReference<Queue<Runnable>> delayedInitTasks;
    private volatile List<ProjectExtensionRef> extensionRefs;
    private volatile Lookup extensionsOnLookup;

    private final AtomicReference<BuiltInGradleCommandQuery> mergedCommandQueryRef;

    private NbGradleProject(FileObject projectDir, ProjectState state) throws IOException {
        this.projectDir = projectDir;
        this.projectDirAsFile = FileUtil.toFile(projectDir);
        if (projectDirAsFile == null) {
            throw new IOException("Project directory does not exist.");
        }

        this.mergedCommandQueryRef = new AtomicReference<BuiltInGradleCommandQuery>(null);
        this.delayedInitTasks = new AtomicReference<Queue<Runnable>>(new LinkedBlockingQueue<Runnable>());
        this.state = state;
        this.defaultLookupRef = new AtomicReference<Lookup>(null);
        this.properties = new ProjectPropertiesProxy(this);
        this.projectInfoManager = new ProjectInfoManager();

        this.hasModelBeenLoaded = new AtomicBoolean(false);
        this.loadErrorRef = new AtomicReference<ProjectInfoRef>(null);
        this.modelChanges = new ChangeSupport(this);
        this.currentModelRef = new AtomicReference<NbGradleModelRef>(
                new NbGradleModelRef(GradleModelLoader.createEmptyModel(projectDirAsFile)));

        this.loadedAtLeastOnceSignal = new WaitableSignal();
        this.name = projectDir.getNameExt();
        this.exceptionDisplayer = new ExceptionDisplayer(NbStrings.getProjectErrorTitle(name));
        this.extensionRefs = Collections.emptyList();
        this.extensionsOnLookup = Lookup.EMPTY;
        this.lookupRef = new AtomicReference<DynamicLookup>(null);
        this.protectedLookupRef = new AtomicReference<Lookup>(null);
    }

    @Nonnull
    public static NbGradleProject createProject(FileObject projectDir, ProjectState state) throws IOException {
        NbGradleProject project = new NbGradleProject(projectDir, state);
        try {
            Collection<? extends GradleProjectExtensionQuery> extensionQueries
                    = Lookup.getDefault().lookupAll(GradleProjectExtensionQuery.class);

            List<GradleProjectExtension> extensions = new LinkedList<GradleProjectExtension>();
            for (GradleProjectExtensionQuery extension: extensionQueries) {
                GradleProjectExtension loadedExtension = null;
                try {
                    loadedExtension = extension.loadExtensionForProject(project);
                } catch (IOException ex) {
                    String errorMessage = "Failed to load a Gradle extension ["
                            + extension.getClass().getName()
                            + "] for this project: "
                            + projectDir;
                    LOGGER.log(Level.INFO, errorMessage, ex);
                } catch (Throwable ex) {
                    String errorMessage = "An unexpected failure prevented loading of a Gradle extension ["
                            + extension.getClass().getName()
                            + "] for this project: "
                            + projectDir;
                    LOGGER.log(Level.SEVERE, errorMessage, ex);
                }

                if (loadedExtension != null) {
                    extensions.add(loadedExtension);
                }
            }
            project.setExtensions(extensions);
        } finally {
            Queue<Runnable> taskList = project.delayedInitTasks.getAndSet(null);
            for (Runnable tasks: taskList) {
                tasks.run();
            }
        }
        return project;
    }

    @Nonnull
    public BuiltInGradleCommandQuery getMergedCommandQuery() {
        BuiltInGradleCommandQuery result = mergedCommandQueryRef.get();
        if (result == null) {
            result = new MergedBuiltInGradleCommandQuery(this);
            mergedCommandQueryRef.compareAndSet(null, result);
            result = mergedCommandQueryRef.get();
        }
        return result;
    }

    @Nonnull
    public List<ProjectExtensionRef> getExtensionRefs() {
        return extensionRefs;
    }

    private void setExtensions(List<GradleProjectExtension> extensions) {
        List<GradleProjectExtension> newExtensions
                = Collections.unmodifiableList(new ArrayList<GradleProjectExtension>(extensions));
        List<Lookup> allLookups = new ArrayList<Lookup>(newExtensions.size() + 1);
        List<ProjectExtensionRef> newExtensionRefs
                = new ArrayList<ProjectExtensionRef>(newExtensions.size());

        allLookups.add(getDefaultLookup());
        for (final GradleProjectExtension extension: newExtensions) {
            allLookups.add(extension.getExtensionLookup());
            newExtensionRefs.add(new ProjectExtensionRef(extension));
        }

        this.extensionsOnLookup = Lookups.fixed(newExtensions.toArray());
        this.extensionRefs = Collections.unmodifiableList(newExtensionRefs);
        getMainLookup().replaceLookups(allLookups);
    }

    public <T extends GradleProjectExtension> T lookupExtension(Class<T> extClass) {
        return extensionsOnLookup.lookup(extClass);
    }

    public NbGradleConfiguration getCurrentProfile() {
        return getLookup().lookup(NbGradleSingleProjectConfigProvider.class).getActiveConfiguration();
    }

    public void addProfileChangeListener(ChangeListener listener) {
        getLookup().lookup(NbGradleSingleProjectConfigProvider.class).addActiveConfigChangeListener(listener);
    }

    public void removeProfileChangeListener(ChangeListener listener) {
        getLookup().lookup(NbGradleSingleProjectConfigProvider.class).removeActiveConfigChangeListener(listener);
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

    @Nonnull
    public TaskVariableMap getVarReplaceMap(Lookup actionContext) {
        final List<TaskVariableMap> maps = new LinkedList<TaskVariableMap>();
        maps.add(StandardTaskVariable.createVarReplaceMap(this, actionContext));

        Collection<? extends GradleTaskVariableQuery> taskVariables
                = getLookup().lookupAll(GradleTaskVariableQuery.class);
        for (GradleTaskVariableQuery query: taskVariables) {
            maps.add(query.getVariableMap(actionContext));
        }
        return new TaskVariableMap() {
            @Override
            public String tryGetValueForVariable(TaskVariable variable) {
                for (TaskVariableMap map: maps) {
                    String value = map.tryGetValueForVariable(variable);
                    if (value != null) {
                        return value;
                    }
                }
                return null;
            }
        };
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
        NbGradleModelRef resultRef = currentModelRef.get();
        NbGradleModel result = resultRef.model;
        // This is not a completely correct solution. The correct
        // solution would be to listen when the model becomes dirty (based on
        // the directory of the project). The problem is that there is no place
        // to unregister such listener.
        //
        // However this should work in most practical cases since
        // getAvailableModel() often gets called.
        if (result.isDirty() || !resultRef.isUpdateToDate()) {
            // Set a non-dirty to prevent many unnecessary project reload.
            currentModelRef.set(new NbGradleModelRef(result.createNonDirtyCopy()));
            reloadProject(true);
        }
        return result;
    }

    public NbGradleModel getCurrentModel() {
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

    private static void checkCanWaitForProjectLoad() {
        if (GradleDaemonManager.isRunningExclusiveTask()) {
            throw new IllegalStateException("Cannot wait for loading a project"
                    + " while blocking daemon tasks from being executed."
                    + " Possible dead-lock.");
        }
    }

    public boolean tryWaitForLoadedProject(long timeout, TimeUnit unit) {
        checkCanWaitForProjectLoad();

        // Ensure that the project is started to be loaded.
        getCurrentModel();
        return loadedAtLeastOnceSignal.tryWaitForSignal(timeout, unit);
    }

    public boolean tryWaitForLoadedProject() {
        checkCanWaitForProjectLoad();

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

    private boolean isInitialized() {
        return delayedInitTasks.get() == null;
    }

    private void runDelayedInitTask(final Runnable task) {
        assert task != null;

        Queue<Runnable> taskList = delayedInitTasks.get();
        if (taskList == null) {
            task.run();
            return;
        }

        final AtomicBoolean executed = new AtomicBoolean(false);
        Runnable delayedTask = new Runnable() {
            @Override
            public void run() {
                if (executed.compareAndSet(false, true)) {
                    task.run();
                }
            }
        };

        taskList.add(delayedTask);
        if (delayedInitTasks.get() == null) {
            delayedTask.run();
        }
    }

    private void loadProject(final boolean onlyIfNotLoaded, final boolean mayUseCache) {
        if (!hasModelBeenLoaded.compareAndSet(false, true)) {
            if (onlyIfNotLoaded) {
                return;
            }
        }

        if (!isInitialized()) {
            runDelayedInitTask(new Runnable() {
                @Override
                public void run() {
                    loadProject(false, mayUseCache);
                }
            });
            return;
        }

        getPropertiesForProfile(getCurrentProfile().getProfileDef(), true, new PropertiesLoadListener() {
            @Override
            public void loadedProperties(ProjectProperties properties) {
                GradleModelLoader.fetchModel(NbGradleProject.this, mayUseCache, new ModelRetrievedListenerImpl());
            }
        });
    }

    public ProjectProperties getProperties() {
        return properties;
    }

    public ProjectProperties getPrivateProperties() {
        ProfileDef privateDef = new ProfileDef("private", "aux-config", "Private profiles");
        return getPropertiesForProfile(privateDef, false, null);
    }

    public ProjectProperties getPropertiesForProfile(
            ProfileDef profileDef,
            boolean useInheritance,
            PropertiesLoadListener onLoadTask) {

        if (useInheritance) {
            return ProjectPropertiesManager.getPropertySourceForProject(this, profileDef).load(onLoadTask);
        }
        else {
            File profileFile = SettingsFiles.getProfileFile(this, profileDef);
            return ProjectPropertiesManager.getFilePropertySource(this, profileFile).load(onLoadTask);
        }
    }

    public ProjectProperties tryGetLoadedProperties() {
        if (properties.tryWaitForLoaded()) {
            return properties;
        }
        else {
            return null;
        }
    }

    @Nonnull
    public String getName() {
        return name;
    }

    @Nonnull
    public String getDisplayName() {
        return getAvailableModel().getDisplayName();
    }

    @Nonnull
    public String getDescription() {
        return getAvailableModel().getDescription();
    }

    @Nonnull
    public File getProjectDirectoryAsFile() {
        return projectDirAsFile;
    }

    @Override
    public FileObject getProjectDirectory() {
        return projectDir;
    }

    private Lookup getDefaultLookup() {
        // The Lookup is not created in the constructor, so that we do not need
        // to share "this" in the constructor.
        Lookup result = defaultLookupRef.get();
        if (result == null) {
            GradleAuxiliaryConfiguration auxConfig = new GradleAuxiliaryConfiguration(this);

            Lookup newLookup = Lookups.fixed(new Object[] {
                this,
                state, //allow outside code to mark the project as needing saving
                NbGradleSingleProjectConfigProvider.create(this),
                new GradleProjectInformation(this),
                new GradleProjectLogicalViewProvider(this),
                new GradleActionProvider(this),
                new GradleSharabilityQuery(this),
                new GradleSourceEncodingQuery(this),
                new GradleCustomizer(this),
                new OpenHook(),
                auxConfig,
                new GradleAuxiliaryProperties(auxConfig),
                new GradleTemplateAttrProvider(this),
                new DefaultGradleCommandExecutor(this),
                ProjectPropertiesApi.buildPlatform(getProperties().getPlatform()),
                ProjectPropertiesApi.scriptPlatform(getProperties().getScriptPlatform()),
                ProjectPropertiesApi.sourceEncoding(getProperties().getSourceEncoding()),
                ProjectPropertiesApi.sourceLevel(getProperties().getSourceLevel()),
                new ProjectInfoManager(),

                // FileOwnerQueryImplementation cannot be added to the project's
                // lookup, since NetBeans will ignore it. It must be added
                // using the ServiceProviders annotation. Our implementation is
                // GradleFileOwnerQuery and is added using the annotation.
            });

            if (defaultLookupRef.compareAndSet(null, newLookup)) {
                for (ProjectInitListener listener: newLookup.lookupAll(ProjectInitListener.class)) {
                    listener.onInitProject();
                }

                loadProject(true, true);
            }
            result = defaultLookupRef.get();
        }
        return result;
    }

    private DynamicLookup getMainLookup() {
        DynamicLookup lookup = lookupRef.get();
        if (lookup == null) {
            lookupRef.compareAndSet(null, new DynamicLookup(getDefaultLookup()));
            lookup = lookupRef.get();
        }
        return lookup;
    }

    @Override
    public Lookup getLookup() {
        Lookup lookup = protectedLookupRef.get();
        if (lookup == null) {
            protectedLookupRef.compareAndSet(null, DynamicLookup.viewLookup(getMainLookup()));
            lookup = protectedLookupRef.get();
        }
        return lookup;
    }

    // equals and hashCode is provided, so that NetBeans doesn't load the
    // same project multiple times.

    @Override
    public int hashCode() {
        return 201 + projectDir.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final NbGradleProject other = (NbGradleProject)obj;
        return this.projectDir.equals(other.projectDir);
    }

    // SwingUtilities.invokeLater is used only to guarantee the order of events.
    // Actually any executor which executes tasks in the order they were
    // submitted to it is good (using SwingUtilities.invokeLater was only
    // convenient to use because registering paths is cheap enough).
    private class OpenHook extends ProjectOpenedHook {
        private final ModelLoadListener modelLoadListener;
        private ChangeListener licenseChangeListener;
        private LicenseManager.Ref licenseRef;
        private boolean opened;

        public OpenHook() {
            this.opened = false;

            this.licenseRef = null;
            this.licenseChangeListener = null;

            this.modelLoadListener = new ModelLoadListener() {
                @Override
                public void modelLoaded(NbGradleModel model) {
                    if (getProjectDirectoryAsFile().equals(model.getProjectDir())) {
                        new ModelRetrievedListenerImpl().onComplete(model, null);
                    }
                }
            };
        }

        private void registerLicenseNow() {
            assert SwingUtilities.isEventDispatchThread();

            if (licenseRef != null) {
                licenseRef.unregister();
            }

            licenseRef = LICENSE_MANAGER.registerLicense(
                    NbGradleProject.this,
                    getProperties().getLicenseHeader().getValue());
        }

        public void registerLicense() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (opened) {
                        registerLicenseNow();
                    }
                }
            });
        }

        @Override
        protected void projectOpened() {
            GradleModelLoader.addModelLoadedListener(modelLoadListener);
            reloadProject(true);

            if (licenseChangeListener != null) {
                LOGGER.warning("projectOpened() without close.");
                properties.getLicenseHeader().removeChangeListener(licenseChangeListener);
            }

            licenseChangeListener = new ChangeListener() {
                @Override
                public void stateChanged(ChangeEvent e) {
                    registerLicense();
                }
            };
            properties.getLicenseHeader().addChangeListener(licenseChangeListener);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    opened = true;
                    registerLicenseNow();
                }
            });
        }

        @Override
        protected void projectClosed() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    opened = false;

                    if (licenseRef != null) {
                        licenseRef.unregister();
                        licenseRef = null;
                    }
                }
            });

           if (licenseChangeListener != null) {
                properties.getLicenseHeader().removeChangeListener(licenseChangeListener);
                licenseChangeListener = null;
            }

            GradleModelLoader.removeModelLoadedListener(modelLoadListener);
        }
    }

    private class ModelRetrievedListenerImpl implements ModelRetrievedListener {
        private void fireModelChangeEvent() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    onModelChange();
                }
            });
        }

        private Set<String> safelyLoadExtensions(GradleProjectExtension extension, Lookup lookup) {
            Set<String> conflicts = null;
            try {
                conflicts = extension.modelsLoaded(lookup);
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE,
                        "Extension has thrown an unexpected exception: " + extension.getExtensionName(),
                        ex);
            }

            return conflicts != null ? conflicts : Collections.<String>emptySet();
        }

        private void notifyEmptyModelChange() {
            for (ProjectExtensionRef extensionRef: extensionRefs) {
                safelyLoadExtensions(extensionRef.getExtension(), Lookup.EMPTY);
            }

            fireModelChangeEvent();
        }

        private void notifyModelChange(NbGradleModel model) {
            int setSize = 2 * extensionRefs.size();
            Set<String> disabledExtensions = new HashSet<String>(setSize);
            Map<String, GradleProjectExtension> loadedExtensions
                    = new HashMap<String, GradleProjectExtension>(setSize);

            for (ProjectExtensionRef extensionRef: extensionRefs) {
                GradleProjectExtension extension = extensionRef.getExtension();

                String name = extension.getExtensionName();
                if (disabledExtensions.contains(name)) {
                    continue;
                }

                Set<String> conflicts = safelyLoadExtensions(
                        extension,
                        model.getModelsForExtension(extensionRef.getName()));

                disabledExtensions.addAll(conflicts);
                loadedExtensions.put(name, extension);
            }

            // TODO: What if an extension is disabled and so extensions
            //  conflicting with it can be enabled? Should we consider this case?

            for (String disabled: disabledExtensions) {
                GradleProjectExtension extension = loadedExtensions.get(disabled);
                if (extension != null) {
                    safelyLoadExtensions(extension, Lookup.EMPTY);
                }
            }

            fireModelChangeEvent();
        }

        private void applyModelLoadResults(NbGradleModel model, Throwable error) {
            boolean hasChanged = false;
            if (model != null) {
                NbGradleModelRef newModel = new NbGradleModelRef(model);
                NbGradleModelRef lastModel = currentModelRef.getAndSet(newModel);
                hasChanged = !lastModel.isSameModel(newModel);
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
                if (model == null) {
                    notifyEmptyModelChange();
                }
                else {
                    notifyModelChange(model);
                }
            }
        }

        @Override
        public void onComplete(NbGradleModel model, Throwable error) {
            try {
                applyModelLoadResults(model, error);
            } finally {
                loadedAtLeastOnceSignal.signal();
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
                        if (exception != null) {
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

    private static final class NbGradleModelRef {
        public final NbGradleModel model;
        private final Object stateID;

        public NbGradleModelRef(NbGradleModel model) {
            this.model = model;
            this.stateID = model.getStateID();
        }

        public boolean isSameModel(NbGradleModelRef other) {
            if (other != this) {
                return false;
            }

            return stateID.equals(other.stateID);
        }

        public boolean isUpdateToDate() {
            return stateID.equals(model.getStateID());
        }
    }
}
