package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.entry.GradleProjectIDs;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.GradleTaskVariableQuery;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.model.ModelLoadListener;
import org.netbeans.gradle.project.model.ModelRefreshListener;
import org.netbeans.gradle.project.model.ModelRetrievedListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.issue.ModelLoadIssue;
import org.netbeans.gradle.project.model.issue.ModelLoadIssueReporter;
import org.netbeans.gradle.project.properties.ActiveSettingsQueryEx;
import org.netbeans.gradle.project.properties.ActiveSettingsQueryListener;
import org.netbeans.gradle.project.properties.GradleAuxiliaryConfiguration;
import org.netbeans.gradle.project.properties.GradleAuxiliaryProperties;
import org.netbeans.gradle.project.properties.GradleCustomizer;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties.MultiProfileProperties;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.NbGradleSingleProjectConfigProvider;
import org.netbeans.gradle.project.properties.ProfileKey;
import org.netbeans.gradle.project.properties.ProfileSettingsContainer;
import org.netbeans.gradle.project.properties.ProfileSettingsKey;
import org.netbeans.gradle.project.properties.ProjectProfileSettings;
import org.netbeans.gradle.project.properties.ProjectPropertiesApi;
import org.netbeans.gradle.project.query.GradleCacheBinaryForSourceQuery;
import org.netbeans.gradle.project.query.GradleCacheByBinaryLookup;
import org.netbeans.gradle.project.query.GradleSharabilityQuery;
import org.netbeans.gradle.project.query.GradleSourceEncodingQuery;
import org.netbeans.gradle.project.query.GradleTemplateAttrProvider;
import org.netbeans.gradle.project.tasks.CombinedTaskVariableMap;
import org.netbeans.gradle.project.tasks.DefaultGradleCommandExecutor;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.netbeans.gradle.project.tasks.MergedBuiltInGradleCommandQuery;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.gradle.project.view.GradleProjectLogicalViewProvider;
import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class NbGradleProject implements Project {
    private static final Logger LOGGER = Logger.getLogger(NbGradleProject.class.getName());

    // Note: There is a lot of assumption on that this executor is
    //   single-threaded and executes task in the order they were submitted.
    public static final MonitorableTaskExecutorService PROJECT_PROCESSOR
            = NbTaskExecutors.newExecutor("Gradle-Project-Processor", 1);
    private static final LicenseManager LICENSE_MANAGER = new LicenseManager();

    private final FileObject projectDir;
    private final File projectDirAsFile;
    private final ProjectState state;
    private final AtomicReference<Lookup> defaultLookupRef;
    private final AtomicReference<DynamicLookup> lookupRef;
    private final DynamicLookup combinedExtensionLookup;

    private final String name;
    private final ChangeListenerManager modelChangeListeners;
    private final AtomicBoolean hasModelBeenLoaded;
    private final AtomicReference<NbGradleModel> currentModelRef;
    private final ProjectInfoManager projectInfoManager;

    private final AtomicReference<ProjectInfoRef> loadErrorRef;

    private final WaitableSignal loadedAtLeastOnceSignal;

    private volatile List<NbGradleExtensionRef> extensionRefs;
    private volatile Set<String> extensionNames;

    private final AtomicReference<BuiltInGradleCommandQuery> mergedCommandQueryRef;

    private NbGradleProject(FileObject projectDir, ProjectState state) throws IOException {
        this.projectDir = projectDir;
        this.projectDirAsFile = FileUtil.toFile(projectDir);
        if (projectDirAsFile == null) {
            throw new IOException("Project directory does not exist.");
        }

        this.mergedCommandQueryRef = new AtomicReference<>(null);
        this.state = state;
        this.defaultLookupRef = new AtomicReference<>(null);
        this.projectInfoManager = new ProjectInfoManager();
        this.combinedExtensionLookup = new DynamicLookup();

        this.hasModelBeenLoaded = new AtomicBoolean(false);
        this.loadErrorRef = new AtomicReference<>(null);
        this.modelChangeListeners = new GenericChangeListenerManager();
        this.currentModelRef = new AtomicReference<>(
                GradleModelLoader.createEmptyModel(projectDirAsFile));

        this.loadedAtLeastOnceSignal = new WaitableSignal();
        this.name = projectDir.getNameExt();
        this.extensionRefs = Collections.emptyList();
        this.extensionNames = Collections.emptySet();
        this.lookupRef = new AtomicReference<>(null);
    }

    @Nonnull
    public static NbGradleProject createProject(FileObject projectDir, ProjectState state) throws IOException {
        NbGradleProject project = new NbGradleProject(projectDir, state);
        project.setExtensions(ExtensionLoader.loadExtensions(project));
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
    public List<NbGradleExtensionRef> getExtensionRefs() {
        return extensionRefs;
    }

    public boolean isSameProject(Project other) {
        return Objects.equals(other.getProjectDirectory(), getProjectDirectory());
    }

    private List<Lookup> extractLookupsFromProviders(Lookup providerContainer) {
        Lookup baseContext = getDefaultLookup();
        // baseContext must contain the Project instance.

        List<Lookup> result = new LinkedList<>();
        for (LookupProvider provider: providerContainer.lookupAll(LookupProvider.class)) {
            result.add(provider.createAdditionalLookup(baseContext));
        }

        return result;
    }

    private List<Lookup> getLookupsFromAnnotations() {
        Lookup lookupProviders = Lookups.forPath("Projects/" + GradleProjectIDs.MODULE_NAME + "/Lookup");
        return extractLookupsFromProviders(lookupProviders);
    }

    private void updateCombinedExtensionLookup() {
        List<Lookup> extensionLookups = new ArrayList<>(extensionRefs.size());
        for (NbGradleExtensionRef extenion: extensionRefs) {
            extensionLookups.add(extenion.getExtensionLookup());
        }
        combinedExtensionLookup.replaceLookups(extensionLookups);
    }

    private static List<LookupProvider> moveToLookupProvider(List<Lookup> lookups) {
        List<LookupProvider> result = new ArrayList<>(lookups.size());
        for (Lookup lookup: lookups) {
            result.add(moveToLookupProvider(lookup));
        }
        return result;
    }

    private static LookupProvider moveToLookupProvider(final Lookup lookup) {
        ExceptionHelper.checkNotNullArgument(lookup, "lookup");
        return new LookupProvider() {
            @Override
            public Lookup createAdditionalLookup(Lookup baseContext) {
                return lookup;
            }
        };
    }

    private void setExtensions(List<NbGradleExtensionRef> extensions) {
        List<LookupProvider> allLookupProviders = new ArrayList<>(extensions.size() + 2);

        Set<String> newExtensionNames = CollectionUtils.newHashSet(extensions.size());


        allLookupProviders.add(moveToLookupProvider(getDefaultLookup()));
        for (final NbGradleExtensionRef extension: extensions) {
            newExtensionNames.add(extension.getName());
            allLookupProviders.add(moveToLookupProvider(extension.getProjectLookup()));
        }

        allLookupProviders.addAll(moveToLookupProvider(getLookupsFromAnnotations()));

        this.extensionNames = Collections.unmodifiableSet(newExtensionNames);
        this.extensionRefs = Collections.unmodifiableList(new ArrayList<>(extensions));

        Lookup combinedLookupProviders = LookupProviderSupport.createCompositeLookup(Lookup.EMPTY, Lookups.fixed(allLookupProviders.toArray()));
        final Lookup combinedAllLookups = new ProxyLookup(combinedLookupProviders);
        getMainLookup().replaceLookups(new ProjectLookupHack(new ProjectLookupHack.LookupContainer() {
            @Override
            public NbGradleProject getProject() {
                return NbGradleProject.this;
            }

            @Override
            public Lookup getLookup() {
                return combinedAllLookups;
            }

            @Override
            public Lookup getLookupAndActivate() {
                ensureLoadRequested();
                return combinedAllLookups;
            }
        }));
        updateCombinedExtensionLookup();
    }

    public boolean hasExtension(String extensionName) {
        return extensionNames.contains(extensionName);
    }

    public Lookup getCombinedExtensionLookup() {
        return combinedExtensionLookup;
    }

    private NbGradleSingleProjectConfigProvider getConfigProvider() {
        // TODO: Cache this value
        return getLookup().lookup(NbGradleSingleProjectConfigProvider.class);
    }

    public NbGradleConfiguration getCurrentProfile() {
        return getConfigProvider().getActiveConfiguration();
    }

    public ListenerRef addProfileChangeListener(Runnable listener) {
        return getConfigProvider().addActiveConfigChangeListener(listener);
    }

    public void displayError(String errorText, Throwable exception) {
        if (!ModelLoadIssueReporter.reportIfBuildScriptError(this, exception)) {
            ModelLoadIssueReporter.reportAllIssues(errorText, Collections.singleton(
                    new ModelLoadIssue(this, null, null, null, exception)));
        }
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
        final List<TaskVariableMap> maps = new LinkedList<>();

        Collection<? extends GradleTaskVariableQuery> taskVariables
                = getCombinedExtensionLookup().lookupAll(GradleTaskVariableQuery.class);
        for (GradleTaskVariableQuery query: taskVariables) {
            maps.add(query.getVariableMap(actionContext));
        }

        // Allow extensions to redefine variables.
        maps.add(StandardTaskVariable.createVarReplaceMap(this, actionContext));

        return new CombinedTaskVariableMap(maps);
    }

    public ProjectInfoManager getProjectInfoManager() {
        return projectInfoManager;
    }

    public ListenerRef addModelChangeListener(Runnable listener) {
        return modelChangeListeners.registerListener(listener);
    }

    public NbGradleModel getAvailableModel() {
        return currentModelRef.get();
    }

    public NbGradleModel getCurrentModel() {
        return getAvailableModel();
    }

    public void tryUpdateFromCache(NbGradleModel baseModel) {
        // In this case we don't yet requested a model, so there is little
        // reason to do anything now: Be lazy!
        if (!hasModelBeenLoaded.get()) {
            return;
        }

        GradleModelLoader.tryUpdateFromCache(this, baseModel, new ModelRetrievedListenerImpl());
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
        return tryWaitForLoadedProject(Cancellation.UNCANCELABLE_TOKEN, timeout, unit);
    }

    public boolean tryWaitForLoadedProject(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        checkCanWaitForProjectLoad();

        // Ensure that the project is started to be loaded.
        getCurrentModel();
        return loadedAtLeastOnceSignal.tryWaitSignal(cancelToken, timeout, unit);
    }

    public void waitForLoadedProject(CancellationToken cancelToken) {
        checkCanWaitForProjectLoad();

        // Ensure that the project is started to be loaded.
        getCurrentModel();
        loadedAtLeastOnceSignal.waitSignal(cancelToken);
    }

    private void onModelChange() {
        assert SwingUtilities.isEventDispatchThread();

        try {
            modelChangeListeners.fireEventually();
        } finally {
            GradleCacheByBinaryLookup.notifyCacheChange();
            GradleCacheBinaryForSourceQuery.notifyCacheChange();
        }
    }

    public void ensureLoadRequested() {
        loadProject(true, true);
    }

    private void loadProject(final boolean onlyIfNotLoaded, final boolean mayUseCache) {
        if (!hasModelBeenLoaded.compareAndSet(false, true)) {
            if (onlyIfNotLoaded) {
                return;
            }
        }

        GradleModelLoader.fetchModel(NbGradleProject.this, mayUseCache, new ModelRetrievedListenerImpl());
    }

    public NbGradleCommonProperties getCommonProperties() {
        return getLookup().lookup(NbGradleCommonProperties.class);
    }

    public NbGradleCommonProperties loadCommonPropertiesForProfile(ProfileKey profileKey) {
        ActiveSettingsQueryEx settings = loadActiveSettingsForProfile(profileKey);
        return new NbGradleCommonProperties(this, settings);
    }

    private List<ProjectProfileSettings> getUnloadedProjectProfileSettingsForProfile(ProfileKey profileKey) {
        List<ProfileSettingsKey> keys = getProjectProfileKey(profileKey).getWithFallbacks();
        return getConfigProvider().getProfileSettingsContainer().getAllProfileSettings(keys);
    }

    public ActiveSettingsQueryEx loadActiveSettingsForProfile(ProfileKey profileKey) {
        List<ProjectProfileSettings> settings = getUnloadedProjectProfileSettingsForProfile(profileKey);
        for (ProjectProfileSettings current: settings) {
            current.ensureLoadedAndWait();
        }

        MultiProfileProperties result = new MultiProfileProperties();
        result.setProfileSettings(settings);
        return result;
    }

    public ListenerRef loadActiveSettingsForProfile(ProfileKey profileKey, final ActiveSettingsQueryListener listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final List<ProjectProfileSettings> settings = getUnloadedProjectProfileSettingsForProfile(profileKey);
        final AtomicInteger requiredCallCount = new AtomicInteger(settings.size());

        final MultiProfileProperties result = new MultiProfileProperties();
        Runnable listenerForwarder = new Runnable() {
            @Override
            public void run() {
                if (requiredCallCount.decrementAndGet() == 0) {
                    result.setProfileSettings(settings);
                    listener.onLoad(result);
                }
            }
        };

        List<ListenerRef> resultRefs = new ArrayList<>(settings.size());
        for (ProjectProfileSettings current: settings) {
            resultRefs.add(current.notifyWhenLoaded(listenerForwarder));
        }
        return ListenerRegistries.combineListenerRefs(resultRefs);
    }

    public ProjectProfileSettings getUnloadedProfileSettings(ProfileKey profileKey) {
        return getProfileSettingsContainer().getProfileSettings(getProjectProfileKey(profileKey));
    }

    public ActiveSettingsQueryEx getActiveSettingsQuery() {
        return getConfigProvider().getActiveSettingsQuery();
    }

    public ProfileSettingsContainer getProfileSettingsContainer() {
        return getConfigProvider().getProfileSettingsContainer();
    }

    public ProfileSettingsKey getProjectProfileKey(ProfileKey profileKey) {
        Path rootProjectDir = getCurrentModel().getRootProjectDir().toPath();
        return new ProfileSettingsKey(rootProjectDir, profileKey);
    }

    public ProjectProfileSettings getPropertiesForProfile(ProfileKey profileKey) {
        ProfileSettingsKey key = getProjectProfileKey(profileKey);
        return getProfileSettingsContainer().getProfileSettings(key);
    }

    public ProjectProfileSettings getPrivateProfile() {
        return getPropertiesForProfile(ProfileKey.PRIVATE_PROFILE);
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
            NbGradleSingleProjectConfigProvider configProvider = NbGradleSingleProjectConfigProvider.create(this);

            NbGradleCommonProperties commonProperties
                    = new NbGradleCommonProperties(this, configProvider.getActiveSettingsQuery());

            Lookup newLookup = Lookups.fixed(new Object[] {
                this,
                state, //allow outside code to mark the project as needing saving
                configProvider,
                commonProperties,
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
                ProjectPropertiesApi.buildPlatform(commonProperties.targetPlatform().getActiveSource()),
                ProjectPropertiesApi.scriptPlatform(commonProperties.scriptPlatform().getActiveSource()),
                ProjectPropertiesApi.sourceEncoding(commonProperties.sourceEncoding().getActiveSource()),
                ProjectPropertiesApi.sourceLevel(commonProperties.sourceLevel().getActiveSource()),
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
        return getMainLookup();
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
        private final ListenerRegistrations openedRefs;
        private LicenseManager.Ref licenseRef;
        private boolean opened;

        public OpenHook() {
            this.opened = false;

            this.licenseRef = null;
            this.openedRefs = new ListenerRegistrations();
        }

        private void registerLicenseNow() {
            assert SwingUtilities.isEventDispatchThread();

            if (licenseRef != null) {
                licenseRef.unregister();
            }

            licenseRef = LICENSE_MANAGER.registerLicense(
                    NbGradleProject.this,
                    getCommonProperties().licenseHeaderInfo().getActiveValue());
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
            openedRefs.unregisterAll();

            openedRefs.add(GradleModelLoader.addModelLoadedListener(new ModelLoadListener() {
                @Override
                public void modelLoaded(NbGradleModel model) {
                    if (getProjectDirectoryAsFile().equals(model.getProjectDir())) {
                        new ModelRetrievedListenerImpl().onComplete(model, null);
                    }
                }
            }));
            reloadProject(true);

            PropertySource<LicenseHeaderInfo> licenseHeaderInfo = getCommonProperties().licenseHeaderInfo().getActiveSource();

            openedRefs.add(licenseHeaderInfo.addChangeListener(new Runnable() {
                @Override
                public void run() {
                    registerLicense();
                }
            }));

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
            openedRefs.unregisterAll();

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

        private boolean safelyLoadExtensions(NbGradleExtensionRef extension, Object model) {
            try {
                return extension.setModelForExtension(model);
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE,
                        "Extension has thrown an unexpected exception: " + extension.getName(),
                        ex);
                return false;
            }
        }

        private boolean notifyEmptyModelChange() {
            boolean changedAny = false;
            for (NbGradleExtensionRef extensionRef: extensionRefs) {
                boolean changed = safelyLoadExtensions(extensionRef, null);
                changedAny = changedAny || changed;
            }

            fireModelChangeEvent();
            return changedAny;
        }

        private boolean notifyModelChange(NbGradleModel model) {
            // TODO: Consider conflicts
            //   GradleProjectExtensionDef.getSuppressedExtensions()

            boolean changedAny = false;
            for (NbGradleExtensionRef extensionRef: extensionRefs) {
                boolean changed = safelyLoadExtensions(extensionRef, model.getModelOfExtension(extensionRef));
                changedAny = changedAny || changed;
            }

            fireModelChangeEvent();
            return changedAny;
        }

        private void startRefresh(Collection<ModelRefreshListener> listeners) {
            for (ModelRefreshListener listener: listeners) {
                try {
                    listener.startRefresh();
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE,
                            "Failed to call " + listener.getClass().getName() + ".startRefresh()",
                            ex);
                }
            }
        }

        private void endRefresh(Collection<ModelRefreshListener> listeners, boolean extensionsChanged) {
            for (ModelRefreshListener listener: listeners) {
                try {
                    listener.endRefresh(extensionsChanged);
                } catch (Throwable ex) {
                    LOGGER.log(Level.SEVERE,
                            "Failed to call " + listener.getClass().getName() + ".endRefresh(" + extensionsChanged + ")",
                            ex);
                }
            }
        }

        private void updateExtensionActivation(NbGradleModel model) {
            Collection<ModelRefreshListener> refreshListeners
                    = new ArrayList<>(getLookup().lookupAll(ModelRefreshListener.class));

            boolean extensionsChanged = false;

            startRefresh(refreshListeners);
            try {
                if (model == null) {
                    extensionsChanged = notifyEmptyModelChange();
                }
                else {
                    extensionsChanged = notifyModelChange(model);
                }
            } finally {
                endRefresh(refreshListeners, extensionsChanged);
            }
        }

        private void applyModelLoadResults(NbGradleModel model, Throwable error) {
            boolean hasChanged = false;
            if (model != null) {
                NbGradleModel prevModel = currentModelRef.getAndSet(model);
                hasChanged = prevModel != model;
            }

            if (error != null) {
                ProjectInfo.Entry entry = new ProjectInfo.Entry(
                        ProjectInfo.Kind.ERROR,
                        NbStrings.getErrorLoadingProject(error));
                getLoadErrorRef().setInfo(new ProjectInfo(Collections.singleton(entry)));
                LOGGER.log(Level.INFO, "Error while loading the project model.", error);
                displayError(NbStrings.getProjectLoadFailure(name), error);
            }
            else {
                getLoadErrorRef().setInfo(null);
            }

            if (hasChanged) {
                updateExtensionActivation(model);
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
}
