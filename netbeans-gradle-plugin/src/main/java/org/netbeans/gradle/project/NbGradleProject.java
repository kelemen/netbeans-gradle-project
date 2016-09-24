package org.netbeans.gradle.project;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery;
import org.netbeans.gradle.project.api.task.GradleCommandExecutor;
import org.netbeans.gradle.project.extensions.ExtensionLoader;
import org.netbeans.gradle.project.model.DefaultGradleModelLoader;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.SettingsGradleDef;
import org.netbeans.gradle.project.model.issue.ModelLoadIssue;
import org.netbeans.gradle.project.model.issue.ModelLoadIssueReporter;
import org.netbeans.gradle.project.properties.DefaultProjectSettingsProvider;
import org.netbeans.gradle.project.properties.GradleAuxiliaryConfiguration;
import org.netbeans.gradle.project.properties.GradleAuxiliaryProperties;
import org.netbeans.gradle.project.properties.GradleCustomizer;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.NbGradleSingleProjectConfigProvider;
import org.netbeans.gradle.project.properties.ProjectProfileLoader;
import org.netbeans.gradle.project.properties.ProjectPropertiesApi;
import org.netbeans.gradle.project.query.GradleSharabilityQuery;
import org.netbeans.gradle.project.query.GradleSourceEncodingQuery;
import org.netbeans.gradle.project.query.GradleTemplateAttrProvider;
import org.netbeans.gradle.project.tasks.DefaultGradleCommandExecutor;
import org.netbeans.gradle.project.tasks.MergedBuiltInGradleCommandQuery;
import org.netbeans.gradle.project.util.CloseableActionContainer;
import org.netbeans.gradle.project.util.LazyValue;
import org.netbeans.gradle.project.util.NbSupplier;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.gradle.project.view.GradleProjectLogicalViewProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class NbGradleProject implements Project {
    private final FileObject projectDir;
    private final File projectDirAsFile;
    private final Path projectDirAsPath;

    private final AtomicReference<ServiceObjects> serviceObjectsRef;
    private volatile NbGradleProjectExtensions extensions;

    private final String name;
    private final LazyValue<PropertySource<String>> displayNameRef;
    private final PropertySource<String> description;
    private final LazyValue<DefaultGradleModelLoader> modelLoaderRef;
    private final ProjectModelManager modelLoadListener;
    private final ProjectModelUpdater<NbGradleModel> modelUpdater;
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
        this.extensions = NbGradleProjectExtensions.EMPTY;

        this.name = projectDir.getNameExt();
        this.modelLoaderRef = new LazyValue<>(new NbSupplier<DefaultGradleModelLoader>() {
            @Override
            public DefaultGradleModelLoader get() {
                return createModelLoader();
            }
        });

        this.modelLoadListener = new ProjectModelManager(this, DefaultGradleModelLoader.createEmptyModel(this.projectDirAsFile));
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
        project.setExtensions(new NbGradleProjectExtensions(ExtensionLoader.loadExtensions(project)));

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

    private void setExtensions(NbGradleProjectExtensions newExtensions) {
        this.extensions = newExtensions;
        getServiceObjects().projectLookups.updateExtensions(this, newExtensions);
    }

    public NbGradleProjectExtensions getExtensions() {
        return extensions;
    }

    public NbGradleSingleProjectConfigProvider getConfigProvider() {
        return getServiceObjects().configProvider;
    }

    public ProjectProfileLoader getProfileLoader() {
        return getServiceObjects().profileLoader;
    }

    public void displayError(String errorText, Throwable exception) {
        if (!ModelLoadIssueReporter.reportIfBuildScriptError(this, exception)) {
            ModelLoadIssueReporter.reportAllIssues(errorText, Collections.singleton(
                    new ModelLoadIssue(this, null, null, null, exception)));
        }
    }

    public ProjectInfoManager getProjectInfoManager() {
        return getServiceObjects().projectInfoManager;
    }

    public PropertySource<NbGradleModel> currentModel() {
        return modelLoadListener.currentModel();
    }

    private Path getPreferredSettingsFile() {
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

    public GradleCommandExecutor getGradleCommandExecutor() {
        return getServiceObjects().commandExecutor;
    }

    public NbGradleCommonProperties getCommonProperties() {
        return getServiceObjects().commonProperties;
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

    public void tryReplaceModel(NbGradleModel model) {
        if (getProjectDirectoryAsFile().equals(model.getProjectDir())) {
            modelLoadListener.updateModel(model, null);
        }
    }

    @Override
    public Lookup getLookup() {
        return getServiceObjects().projectLookups.getMainLookup();
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

    private static final class ServiceObjects {
        public final GradleAuxiliaryConfiguration auxConfig;
        public final NbGradleSingleProjectConfigProvider configProvider;
        public final ProjectProfileLoader profileLoader;
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
        public final NbGradleProjectLookups projectLookups;

        public ServiceObjects(NbGradleProject project, ProjectState state) {
            List<Object> serviceObjects = new LinkedList<>();
            serviceObjects.add(project);

            this.configProvider = add(NbGradleSingleProjectConfigProvider.create(project), serviceObjects);
            this.profileLoader = new ProjectProfileLoader(configProvider);
            this.commonProperties = configProvider.getCommonProperties(configProvider.getActiveSettingsQuery());

            this.auxConfig = add(new GradleAuxiliaryConfiguration(profileLoader), serviceObjects);
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

            add(ProjectPropertiesApi.buildPlatform(commonProperties.targetPlatform().getActiveSource()), serviceObjects);
            add(ProjectPropertiesApi.scriptPlatform(commonProperties.scriptPlatform().getActiveSource()), serviceObjects);
            add(ProjectPropertiesApi.sourceEncoding(commonProperties.sourceEncoding().getActiveSource()), serviceObjects);
            add(ProjectPropertiesApi.sourceLevel(commonProperties.sourceLevel().getActiveSource()), serviceObjects);

            this.services = Lookups.fixed(serviceObjects.toArray());
            this.projectLookups = new NbGradleProjectLookups(this.services);
        }

        private static <T> T add(T obj, Collection<? super T> serviceContainer) {
            serviceContainer.add(obj);
            return obj;
        }
    }
}
