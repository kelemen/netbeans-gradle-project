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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nonnull;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.config.ActiveSettingsQueryListener;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.api.entry.GradleProjectIDs;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.GradleTaskVariableQuery;
import org.netbeans.gradle.project.api.task.TaskVariable;
import org.netbeans.gradle.project.api.task.TaskVariableMap;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.extensions.ExtensionLoader;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.netbeans.gradle.project.lookups.DynamicLookup;
import org.netbeans.gradle.project.lookups.ProjectLookupHack;
import org.netbeans.gradle.project.model.DefaultGradleModelLoader;
import org.netbeans.gradle.project.model.ModelRefreshListener;
import org.netbeans.gradle.project.model.ModelRetrievedListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.ProjectModelChangeListener;
import org.netbeans.gradle.project.model.SettingsGradleDef;
import org.netbeans.gradle.project.model.issue.ModelLoadIssue;
import org.netbeans.gradle.project.model.issue.ModelLoadIssueReporter;
import org.netbeans.gradle.project.properties.ActiveSettingsQueryEx;
import org.netbeans.gradle.project.properties.DefaultProjectSettingsProvider;
import org.netbeans.gradle.project.properties.GradleAuxiliaryConfiguration;
import org.netbeans.gradle.project.properties.GradleAuxiliaryProperties;
import org.netbeans.gradle.project.properties.GradleCustomizer;
import org.netbeans.gradle.project.properties.MultiProfileProperties;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.NbGradleConfiguration;
import org.netbeans.gradle.project.properties.NbGradleSingleProjectConfigProvider;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.properties.ProfileSettingsContainer;
import org.netbeans.gradle.project.properties.ProfileSettingsKey;
import org.netbeans.gradle.project.properties.ProjectProfileSettingsKey;
import org.netbeans.gradle.project.properties.ProjectPropertiesApi;
import org.netbeans.gradle.project.properties.SingleProfileSettingsEx;
import org.netbeans.gradle.project.properties.standard.CustomVariables;
import org.netbeans.gradle.project.query.GradleCacheBinaryForSourceQuery;
import org.netbeans.gradle.project.query.GradleCacheByBinaryLookup;
import org.netbeans.gradle.project.query.GradleSharabilityQuery;
import org.netbeans.gradle.project.query.GradleSourceEncodingQuery;
import org.netbeans.gradle.project.query.GradleTemplateAttrProvider;
import org.netbeans.gradle.project.tasks.CombinedTaskVariableMap;
import org.netbeans.gradle.project.tasks.DefaultGradleCommandExecutor;
import org.netbeans.gradle.project.tasks.EnvTaskVariableMap;
import org.netbeans.gradle.project.tasks.MergedBuiltInGradleCommandQuery;
import org.netbeans.gradle.project.tasks.StandardTaskVariable;
import org.netbeans.gradle.project.util.CloseableActionContainer;
import org.netbeans.gradle.project.util.LazyValue;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.gradle.project.view.GradleProjectLogicalViewProvider;
import org.netbeans.spi.project.LookupProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.netbeans.spi.project.ui.support.UILookupMergerSupport;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class NbGradleProject implements Project {
    private static final Logger LOGGER = Logger.getLogger(NbGradleProject.class.getName());

    private final FileObject projectDir;
    private final File projectDirAsFile;
    private final Path projectDirAsPath;

    private final AtomicReference<ServiceObjects> serviceObjectsRef;

    private final LazyValue<DynamicLookup> mainLookupRef;
    private final DynamicLookup combinedExtensionLookup;

    private final String name;
    private final LazyValue<PropertySource<String>> displayNameRef;
    private final PropertySource<String> description;
    private final LazyValue<DefaultGradleModelLoader> modelLoaderRef;
    private final ModelRetrievedListenerImpl modelLoadListener;
    private final ProjectModelUpdater<NbGradleModel> modelUpdater;

    private volatile List<NbGradleExtensionRef> extensionRefs;
    private volatile Set<String> extensionNames;

    private final LazyValue<BuiltInGradleCommandQuery> mergedCommandQueryRef;

    private final AtomicReference<Path> preferredSettingsFileRef;

    private NbGradleProject(FileObject projectDir) throws IOException {
        this.projectDir = projectDir;
        this.projectDirAsFile = FileUtil.toFile(projectDir);
        this.projectDirAsPath = projectDirAsFile.toPath();
        if (projectDirAsFile == null) {
            throw new IOException("Project directory does not exist.");
        }
        this.serviceObjectsRef = new AtomicReference<>(null);
        this.preferredSettingsFileRef = new AtomicReference<>(tryGetPreferredSettingsFile(projectDirAsFile));

        this.mergedCommandQueryRef = new LazyValue<>(new NbSupplier<BuiltInGradleCommandQuery>() {
            @Override
            public BuiltInGradleCommandQuery get() {
                return createMergedBuiltInGradleCommandQuery();
            }
        });
        this.combinedExtensionLookup = new DynamicLookup();

        this.name = projectDir.getNameExt();
        this.extensionRefs = Collections.emptyList();
        this.extensionNames = Collections.emptySet();
        this.mainLookupRef = new LazyValue<>(new NbSupplier<DynamicLookup>() {
            @Override
            public DynamicLookup get() {
                return new DynamicLookup(getDefaultLookup());
            }
        });
        this.modelLoaderRef = new LazyValue<>(new NbSupplier<DefaultGradleModelLoader>() {
            @Override
            public DefaultGradleModelLoader get() {
                return createModelLoader();
            }
        });

        this.modelLoadListener = new ModelRetrievedListenerImpl(this);
        final PropertySource<NbGradleModel> currentModel = this.modelLoadListener.currentModel();

        this.displayNameRef = new LazyValue<>(new NbSupplier<PropertySource<String>>() {
            @Override
            public PropertySource<String> get() {
                return getDisplayName(
                        currentModel,
                        getServiceObjects().commonProperties.displayNamePattern().getActiveSource());
            }
        });
        this.description = PropertyFactory.convert(currentModel, new ValueConverter<NbGradleModel, String>() {
            @Override
            public String convert(NbGradleModel input) {
                return input.getDescription();
            }
        });
        this.modelUpdater = new ProjectModelUpdater<>(modelLoaderRef, modelLoadListener);
    }

    private DefaultGradleModelLoader createModelLoader() {
        DefaultGradleModelLoader.Builder result = new DefaultGradleModelLoader.Builder(this);
        return result.create();
    }

    private static Path tryGetPreferredSettingsFile(File projectDir) {
        if (NbGradleModel.isBuildSrcDirectory(projectDir)) {
            return null;
        }

        Path explicitSettingsFile = RootProjectRegistry.getDefault().tryGetSettingsFile(projectDir);
        if (explicitSettingsFile != null) {
            return explicitSettingsFile;
        }

        return NbGradleModel.findSettingsGradle(projectDir);
    }

    private void initServiceObjects(ProjectState state) {
        ServiceObjects serviceObjects = new ServiceObjects(this, state);
        if (!serviceObjectsRef.compareAndSet(null, serviceObjects)) {
            throw new IllegalStateException("Alread initialized: ServiceObjects");
        }

        for (ProjectInitListener listener: serviceObjects.services.lookupAll(ProjectInitListener.class)) {
            listener.onInitProject();
        }
    }

    private ServiceObjects getServiceObjects() {
        ServiceObjects result = serviceObjectsRef.get();
        if (result == null) {
            throw new IllegalStateException("Services are not yet initialized.");
        }
        return result;
    }

    private static PropertySource<String> getDisplayName(
            final PropertySource<NbGradleModel> model,
            final PropertySource<String> namePattern) {

        return new PropertySource<String>() {
            @Override
            public String getValue() {
                return model.getValue().getDisplayName(namePattern.getValue());
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                ListenerRef ref1 = model.addChangeListener(listener);
                ListenerRef ref2 = namePattern.addChangeListener(listener);
                return ListenerRegistries.combineListenerRefs(ref1, ref2);
            }
        };
    }

    @Nonnull
    public static NbGradleProject createProject(FileObject projectDir, ProjectState state) throws IOException {
        NbGradleProject project = new NbGradleProject(projectDir);
        project.initServiceObjects(state);
        project.setExtensions(ExtensionLoader.loadExtensions(project));

        LoadedProjectManager.getDefault().addProject(project);
        project.updateSettingsFile();
        return project;
    }

    private BuiltInGradleCommandQuery createMergedBuiltInGradleCommandQuery() {
        return new MergedBuiltInGradleCommandQuery(this);
    }

    @Nonnull
    public BuiltInGradleCommandQuery getMergedCommandQuery() {
        return mergedCommandQueryRef.get();
    }

    @Nonnull
    public List<NbGradleExtensionRef> getExtensionRefs() {
        return extensionRefs;
    }

    public void ensureLoadRequested() {
        modelUpdater.ensureLoadRequested();
    }

    public void reloadProject() {
        modelUpdater.reloadProject();
    }

    public void waitForLoadedProject(CancellationToken cancelToken) {
        modelUpdater.waitForLoadedProject(cancelToken);
    }

    public boolean tryWaitForLoadedProject(long timeout, TimeUnit unit) {
        return modelUpdater.tryWaitForLoadedProject(timeout, unit);
    }

    public boolean tryWaitForLoadedProject(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        return modelUpdater.tryWaitForLoadedProject(cancelToken, timeout, unit);
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

    private static Lookup getLookupMergers() {
        return Lookups.fixed(
                UILookupMergerSupport.createPrivilegedTemplatesMerger(),
                UILookupMergerSupport.createProjectProblemsProviderMerger(),
                UILookupMergerSupport.createRecommendedTemplatesMerger()
        );
    }

    private void setExtensions(List<NbGradleExtensionRef> extensions) {
        List<LookupProvider> allLookupProviders = new ArrayList<>(extensions.size() + 3);

        Set<String> newExtensionNames = CollectionUtils.newHashSet(extensions.size());


        allLookupProviders.add(moveToLookupProvider(getDefaultLookup()));
        for (final NbGradleExtensionRef extension: extensions) {
            newExtensionNames.add(extension.getName());
            allLookupProviders.add(moveToLookupProvider(extension.getProjectLookup()));
        }

        allLookupProviders.add(moveToLookupProvider(getLookupMergers()));

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
                modelUpdater.ensureLoadRequested();
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
        return getServiceObjects().configProvider;
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

    private static TaskVariableMap asTaskVariableMap(PropertySource<? extends CustomVariables> varsProperty) {
        final CustomVariables vars = varsProperty.getValue();
        if (vars == null || vars.isEmpty()) {
            return null;
        }

        return new TaskVariableMap() {
            @Override
            public String tryGetValueForVariable(TaskVariable variable) {
                return vars.tryGetValue(variable.getVariableName());
            }
        };
    }

    private static void addAsTaskVariableMap(
            PropertySource<? extends CustomVariables> varsProperty,
            List<? super TaskVariableMap> result) {
        TaskVariableMap vars = asTaskVariableMap(varsProperty);
        if (vars != null) {
            result.add(vars);
        }
    }

    @Nonnull
    public TaskVariableMap getVarReplaceMap(Lookup actionContext) {
        final List<TaskVariableMap> maps = new ArrayList<>();

        addAsTaskVariableMap(getCommonProperties().customVariables().getActiveSource(), maps);

        Collection<? extends GradleTaskVariableQuery> taskVariables
                = getCombinedExtensionLookup().lookupAll(GradleTaskVariableQuery.class);
        for (GradleTaskVariableQuery query: taskVariables) {
            maps.add(query.getVariableMap(actionContext));
        }

        // Allow extensions to redefine variables.
        maps.add(StandardTaskVariable.createVarReplaceMap(this, actionContext));
        maps.add(EnvTaskVariableMap.DEFAULT);

        return new CombinedTaskVariableMap(maps);
    }

    public ProjectInfoManager getProjectInfoManager() {
        return getServiceObjects().projectInfoManager;
    }

    public PropertySource<NbGradleModel> currentModel() {
        return modelLoadListener.currentModel();
    }

    public Path getPreferredSettingsFile() {
        return preferredSettingsFileRef.get();
    }

    public SettingsGradleDef getPreferredSettingsGradleDef() {
        return new SettingsGradleDef(
                getPreferredSettingsFile(),
                !currentModel().getValue().isRootWithoutSettingsGradle());
    }

    private void updateSettingsFile(Path settingsFile) {
        Path prevSettingsFile = preferredSettingsFileRef.getAndSet(settingsFile);
        if (Objects.equals(prevSettingsFile, settingsFile)) {
            return;
        }

        modelUpdater.reloadProjectMayUseCache();
    }

    public void updateSettingsFile() {
        updateSettingsFile(tryGetPreferredSettingsFile(getProjectDirectoryAsFile()));
    }

    public ProjectSettingsProvider getProjectSettingsProvider() {
        return getServiceObjects().projectSettingsProvider;
    }

    public NbGradleCommonProperties getCommonProperties() {
        return getServiceObjects().commonProperties;
    }

    public NbGradleCommonProperties loadCommonPropertiesForProfile(ProfileKey profileKey) {
        ActiveSettingsQueryEx settings = loadActiveSettingsForProfile(profileKey);
        return new NbGradleCommonProperties(this, settings);
    }

    private List<ProfileSettingsKey> keysWithFallbacks(ProfileKey profileKey) {
        return getProjectProfileKey(profileKey).getWithFallbacks();
    }

    public ActiveSettingsQueryEx loadActiveSettingsForProfile(ProfileKey profileKey) {
        ProfileSettingsContainer settingsContainer = getConfigProvider().getProfileSettingsContainer();
        List<ProfileSettingsKey> combinedKeys = keysWithFallbacks(profileKey);

        List<SingleProfileSettingsEx> settings = settingsContainer.loadAllProfileSettings(combinedKeys);
        return new MultiProfileProperties(settings);
    }

    public ListenerRef loadActiveSettingsForProfile(ProfileKey profileKey, final ActiveSettingsQueryListener listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        ProfileSettingsContainer settingsContainer = getConfigProvider().getProfileSettingsContainer();
        List<ProfileSettingsKey> combinedKeys = keysWithFallbacks(profileKey);

        return settingsContainer.loadAllProfileSettings(combinedKeys, new NbConsumer<List<SingleProfileSettingsEx>>() {
            @Override
            public void accept(List<SingleProfileSettingsEx> settings) {
                listener.onLoad(new MultiProfileProperties(settings));
            }
        });
    }

    public ActiveSettingsQueryEx getActiveSettingsQuery() {
        return getConfigProvider().getActiveSettingsQuery();
    }

    public ProfileSettingsContainer getProfileSettingsContainer() {
        return getConfigProvider().getProfileSettingsContainer();
    }

    public ProfileSettingsKey getProjectProfileKey(ProfileKey profileKey) {
        return ProjectProfileSettingsKey.getForProject(this, profileKey);
    }

    public SingleProfileSettingsEx loadPropertiesForProfile(ProfileKey profileKey) {
        ProfileSettingsKey key = getProjectProfileKey(profileKey);
        return getProfileSettingsContainer().loadProfileSettings(key);
    }

    public SingleProfileSettingsEx loadPrivateProfile() {
        return loadPropertiesForProfile(ProfileKey.PRIVATE_PROFILE);
    }

    @Nonnull
    public String getName() {
        return name;
    }

    public PropertySource<String> displayName() {
        return displayNameRef.get();
    }

    public PropertySource<String> description() {
        return description;
    }

    @Nonnull
    public Path getProjectDirectoryAsPath() {
        return projectDirAsPath;
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
        return getServiceObjects().services;
    }

    private DynamicLookup getMainLookup() {
        return mainLookupRef.get();
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

    public void tryReplaceModel(NbGradleModel model) {
        if (getProjectDirectoryAsFile().equals(model.getProjectDir())) {
            modelLoadListener.onComplete(model, null);
        }
    }

    private static class OpenHook extends ProjectOpenedHook {
        private final NbGradleProject project;
        private final CloseableActionContainer closeableActions;
        private final AtomicBoolean initialized;

        public OpenHook(NbGradleProject project) {
            this.project = project;
            this.closeableActions = new CloseableActionContainer();
            this.initialized = new AtomicBoolean(false);
        }

        private void ensureInitialized() {
            if (!initialized.compareAndSet(false, true)) {
                return;
            }

            this.closeableActions.defineAction(LicenseManager.getDefault().getRegisterListenerAction(
                    project,
                    project.getCommonProperties().licenseHeaderInfo().getActiveSource()));

            this.closeableActions.defineAction(RootProjectRegistry.getDefault().forProject(project));
        }

        @Override
        protected void projectOpened() {
            ensureInitialized();

            closeableActions.open();
            project.modelUpdater.reloadProjectMayUseCache();
        }

        @Override
        protected void projectClosed() {
            closeableActions.close();
        }
    }

    private static final class ModelRetrievedListenerImpl implements ModelRetrievedListener<NbGradleModel> {
        private final NbGradleProject project;

        private final ChangeListenerManager modelChangeListeners;
        private final AtomicReference<NbGradleModel> currentModelRef;
        private final PropertySource<NbGradleModel> currentModel;
        private final LazyValue<ProjectInfoRef> loadErrorRef;

        private final UpdateTaskExecutor modelUpdater;
        private final Runnable modelUpdateDispatcher;

        public ModelRetrievedListenerImpl(final NbGradleProject project) {
            this.project = project;

            this.modelChangeListeners = GenericChangeListenerManager.getSwingNotifier();
            this.currentModelRef = new AtomicReference<>(
                DefaultGradleModelLoader.createEmptyModel(project.getProjectDirectoryAsFile()));
            this.currentModel = NbProperties.atomicValueView(currentModelRef, modelChangeListeners);

            this.modelUpdater = new SwingUpdateTaskExecutor(true);
            this.modelUpdateDispatcher = new Runnable() {
                @Override
                public void run() {
                    onModelChange();
                }
            };
            this.loadErrorRef = new LazyValue<>(new NbSupplier<ProjectInfoRef>() {
                @Override
                public ProjectInfoRef get() {
                    return project.getProjectInfoManager().createInfoRef();
                }
            });
        }

        private void onModelChange() {
            assert SwingUtilities.isEventDispatchThread();

            try {
                modelChangeListeners.fireEventually();
                for (ProjectModelChangeListener listener: project.getLookup().lookupAll(ProjectModelChangeListener.class)) {
                    listener.onModelChanged();
                }
            } finally {
                GradleCacheByBinaryLookup.notifyCacheChange();
                GradleCacheBinaryForSourceQuery.notifyCacheChange();
            }
        }

        public PropertySource<NbGradleModel> currentModel() {
            return currentModel;
        }

        private void fireModelChangeEvent() {
            modelUpdater.execute(modelUpdateDispatcher);
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
            for (NbGradleExtensionRef extensionRef: project.getExtensionRefs()) {
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
            for (NbGradleExtensionRef extensionRef: project.getExtensionRefs()) {
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
                    = new ArrayList<>(project.getLookup().lookupAll(ModelRefreshListener.class));

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

        private ProjectInfoRef getLoadErrorRef() {
            return loadErrorRef.get();
        }

        @Override
        public void onComplete(NbGradleModel model, Throwable error) {
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
                project.displayError(NbStrings.getProjectLoadFailure(project.getName()), error);
            }
            else {
                getLoadErrorRef().setInfo(null);
            }

            if (hasChanged) {
                updateExtensionActivation(model);
            }
        }
    }

    private static final class ServiceObjects {
        public final GradleAuxiliaryConfiguration auxConfig;
        public final NbGradleSingleProjectConfigProvider configProvider;
        public final NbGradleCommonProperties commonProperties;
        public final ProjectState state;
        public final GradleProjectInformation projectInformation;
        public final GradleProjectLogicalViewProvider logicalViewProvider;
        public final GradleActionProvider actionProvider;
        public final GradleSharabilityQuery sharabilityQuery;
        public final GradleSourceEncodingQuery sourceEncoding;
        public final GradleCustomizer customizer;
        public final GradleAuxiliaryProperties auxProperties;
        public final GradleTemplateAttrProvider templateAttrProvider;
        public final DefaultGradleCommandExecutor commandExecutor;
        public final ProjectInfoManager projectInfoManager;
        public final ProjectSettingsProvider projectSettingsProvider;

        public final Lookup services;

        public ServiceObjects(NbGradleProject project, ProjectState state) {
            List<Object> serviceObjects = new LinkedList<>();
            serviceObjects.add(project);

            this.auxConfig = add(new GradleAuxiliaryConfiguration(project), serviceObjects);
            this.configProvider = add(NbGradleSingleProjectConfigProvider.create(project), serviceObjects);
            this.state = add(state, serviceObjects);
            this.projectInformation = add(new GradleProjectInformation(project), serviceObjects);
            this.logicalViewProvider = add(new GradleProjectLogicalViewProvider(project), serviceObjects);
            this.actionProvider = add(new GradleActionProvider(project), serviceObjects);
            this.sharabilityQuery = add(new GradleSharabilityQuery(project), serviceObjects);
            this.sourceEncoding = add(new GradleSourceEncodingQuery(project), serviceObjects);
            this.customizer = add(new GradleCustomizer(project), serviceObjects);
            this.auxProperties = add(new GradleAuxiliaryProperties(auxConfig), serviceObjects);
            this.templateAttrProvider = add(new GradleTemplateAttrProvider(project), serviceObjects);
            this.commandExecutor = add(new DefaultGradleCommandExecutor(project), serviceObjects);
            this.projectInfoManager = add(new ProjectInfoManager(), serviceObjects);
            this.projectSettingsProvider = add(new DefaultProjectSettingsProvider(project), serviceObjects);

            add(new OpenHook(project), serviceObjects);

            // NbGradleCommonProperties is not needed on the lookup
            this.commonProperties = new NbGradleCommonProperties(project, configProvider.getActiveSettingsQuery());

            add(ProjectPropertiesApi.buildPlatform(commonProperties.targetPlatform().getActiveSource()), serviceObjects);
            add(ProjectPropertiesApi.scriptPlatform(commonProperties.scriptPlatform().getActiveSource()), serviceObjects);
            add(ProjectPropertiesApi.sourceEncoding(commonProperties.sourceEncoding().getActiveSource()), serviceObjects);
            add(ProjectPropertiesApi.sourceLevel(commonProperties.sourceLevel().getActiveSource()), serviceObjects);

            this.services = Lookups.fixed(serviceObjects.toArray());
        }

        private static <T> T add(T obj, Collection<? super T> serviceContainer) {
            serviceContainer.add(obj);
            return obj;
        }
    }
}
