package org.netbeans.gradle.project.java;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.property.swing.SwingForwarderFactory;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.ProjectInfo;
import org.netbeans.gradle.project.ProjectInfoManager;
import org.netbeans.gradle.project.ProjectInfoRef;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.java.model.JavaSourceDirHandler;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.idea.IdeaJavaModelUtils;
import org.netbeans.gradle.project.java.nodes.JavaExtensionNodes;
import org.netbeans.gradle.project.java.nodes.JavaProjectContextActions;
import org.netbeans.gradle.project.java.properties.JavaDebuggingPanel;
import org.netbeans.gradle.project.java.properties.JavaProjectProperties;
import org.netbeans.gradle.project.java.query.GradleAnnotationProcessingQuery;
import org.netbeans.gradle.project.java.query.GradleBinaryForSourceQuery;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.java.query.GradleProjectSources;
import org.netbeans.gradle.project.java.query.GradleProjectTemplates;
import org.netbeans.gradle.project.java.query.GradleSourceForBinaryQuery;
import org.netbeans.gradle.project.java.query.GradleSourceLevelQueryImplementation;
import org.netbeans.gradle.project.java.query.GradleUnitTestFinder;
import org.netbeans.gradle.project.java.query.J2SEPlatformFromScriptQueryImpl;
import org.netbeans.gradle.project.java.query.JavaInitScriptQuery;
import org.netbeans.gradle.project.java.tasks.GradleJavaBuiltInCommands;
import org.netbeans.gradle.project.java.tasks.JavaGradleTaskVariableQuery;
import org.netbeans.gradle.project.model.issue.DependencyResolutionIssue;
import org.netbeans.gradle.project.model.issue.ModelLoadIssueReporter;
import org.netbeans.gradle.project.util.CloseableAction;
import org.netbeans.gradle.project.util.CloseableActionContainer;
import org.netbeans.spi.project.support.LookupProviderSupport;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class JavaExtension implements GradleProjectExtension2<NbJavaModel> {
    private static final Logger LOGGER = Logger.getLogger(JavaExtension.class.getName());

    private final Project project;
    private final File projectDirectoryAsFile;
    private volatile NbJavaModel currentModel;
    private volatile boolean hasEverBeenLoaded;

    private final GradleClassPathProvider cpProvider;
    private final AtomicReference<JavaSourceDirHandler> sourceDirsHandlerRef;
    private final ProjectInfoRef dependencyResolutionFailureRef;

    private final AtomicReference<Lookup> projectLookupRef;
    private final AtomicReference<Lookup> permanentLookupRef;
    private final AtomicReference<Lookup> extensionLookupRef;
    private final AtomicReference<Lookup> combinedLookupRef;

    private final ChangeListenerManager modelChangeListeners;
    private final AtomicReference<JavaProjectProperties> projectPropertiesRef;
    private final AtomicReference<ProjectSettingsProvider.ExtensionSettings> extensionSettingsRef;

    private JavaExtension(Project project) throws IOException {
        ExceptionHelper.checkNotNullArgument(project, "project");

        this.projectDirectoryAsFile = FileUtil.toFile(project.getProjectDirectory());
        if (projectDirectoryAsFile == null) {
            throw new IOException("Project directory does not exist: " + project.getProjectDirectory());
        }

        this.project = project;
        this.currentModel = IdeaJavaModelUtils.createEmptyModel(project.getProjectDirectory());
        this.cpProvider = new GradleClassPathProvider(this);
        this.projectLookupRef = new AtomicReference<>(null);
        this.permanentLookupRef = new AtomicReference<>(null);
        this.extensionLookupRef = new AtomicReference<>(null);
        this.combinedLookupRef = new AtomicReference<>(null);
        this.hasEverBeenLoaded = false;
        this.sourceDirsHandlerRef = new AtomicReference<>(null);
        this.dependencyResolutionFailureRef = getProjectInfoManager(project).createInfoRef();
        this.modelChangeListeners = new GenericChangeListenerManager();
        this.projectPropertiesRef = new AtomicReference<>(null);
        this.extensionSettingsRef = new AtomicReference<>(null);
    }

    public ProjectSettingsProvider.ExtensionSettings getExtensionSettings() {
        ProjectSettingsProvider.ExtensionSettings result = extensionSettingsRef.get();
        if (result == null) {
            ProjectSettingsProvider settingsProvider = project.getLookup().lookup(ProjectSettingsProvider.class);
            if (settingsProvider == null) {
                throw new IllegalArgumentException("Not a Gradle project.");
            }

            result = settingsProvider.getExtensionSettings(JavaExtensionDef.EXTENSION_NAME);

            if (!extensionSettingsRef.compareAndSet(null, result)) {
                result = extensionSettingsRef.get();
            }
        }
        return result;
    }

    public JavaProjectProperties getProjectProperties() {
        JavaProjectProperties result = projectPropertiesRef.get();
        if (result == null) {
            ProjectSettingsProvider.ExtensionSettings extensionSettings = getExtensionSettings();
            result = new JavaProjectProperties(extensionSettings.getActiveSettings());
            if (!projectPropertiesRef.compareAndSet(null, result)) {
                result = projectPropertiesRef.get();
            }
        }
        return result;
    }

    public static JavaExtension getJavaExtensionOfProject(Project project) {
        JavaExtension result = project.getLookup().lookup(JavaExtension.class);
        if (result != null) {
            return result;
        }
        else {
            LOGGER.log(Level.WARNING,
                    "JavaExtension cannot be found the project''s lookup: {0}",
                    project.getProjectDirectory());
            try {
                return create(project);
            } catch (IOException ex) {
                throw new RuntimeException(ex);
            }
        }
    }

    public ListenerRef addModelChangeListener(Runnable listener) {
        return modelChangeListeners.registerListener(listener);
    }

    public JavaSourceDirHandler getSourceDirsHandler() {
        JavaSourceDirHandler result = sourceDirsHandlerRef.get();
        if (result == null) {
            sourceDirsHandlerRef.compareAndSet(null, new JavaSourceDirHandler(this));
            result = sourceDirsHandlerRef.get();
        }
        return result;
    }

    public static JavaExtension create(Project project) throws IOException {
        return new JavaExtension(project);
    }

    public boolean isOwnerProject(File file) {
        FileObject fileObj;

        File currentFile = file;
        fileObj = FileUtil.toFileObject(currentFile);
        while (fileObj == null) {
            currentFile = currentFile.getParentFile();
            if (currentFile == null) {
                return false;
            }

            fileObj = FileUtil.toFileObject(currentFile);
        }

        return isOwnerProject(fileObj);
    }

    public boolean isOwnerProject(FileObject file) {
        Project owner = FileOwnerQuery.getOwner(file);
        if (owner == null) {
            return false;
        }

        return project.getProjectDirectory().equals(owner.getProjectDirectory());
    }

    public NbJavaModel getCurrentModel() {
        return currentModel;
    }

    private void initLookup(Lookup lookup) {
        for (ProjectInitListener listener: lookup.lookupAll(ProjectInitListener.class)) {
            listener.onInitProject();
        }
    }

    // These classes are on the lookup always.
    @Override
    public Lookup getPermanentProjectLookup() {
        Lookup lookup = permanentLookupRef.get();
        if (lookup == null) {
            lookup = Lookups.fixed(this, new OpenHook(this));

            if (permanentLookupRef.compareAndSet(null, lookup)) {
                initLookup(lookup);
            }
            lookup = permanentLookupRef.get();
        }
        return lookup;
    }

    @Override
    public Lookup getProjectLookup() {
        Lookup lookup = projectLookupRef.get();
        if (lookup == null) {
            lookup = Lookups.fixed(
                    LookupProviderSupport.createSourcesMerger(),
                    new GradleProjectSources(this),
                    cpProvider,
                    new GradleSourceLevelQueryImplementation(this),
                    new GradleUnitTestFinder(this),
                    new GradleAnnotationProcessingQuery(),
                    new GradleSourceForBinaryQuery(this),
                    new GradleBinaryForSourceQuery(this),
                    new GradleProjectTemplates(),
                    new JavaGradleTaskVariableQuery(this),
                    new J2SEPlatformFromScriptQueryImpl(this) // internal use only
                    );

            if (projectLookupRef.compareAndSet(null, lookup)) {
                initLookup(lookup);
            }
            lookup = projectLookupRef.get();
        }
        return lookup;
    }

    @Override
    public Lookup getExtensionLookup() {
        Lookup lookup = extensionLookupRef.get();
        if (lookup == null) {
            lookup = Lookups.fixed(new JavaExtensionNodes(this),
                    new JavaProjectContextActions(this),
                    new GradleJavaBuiltInCommands(this),
                    new JavaInitScriptQuery(),
                    JavaDebuggingPanel.createDebuggingCustomizer(this));

            if (extensionLookupRef.compareAndSet(null, lookup)) {
                initLookup(lookup);
            }
            lookup = extensionLookupRef.get();
        }
        return lookup;
    }

    public Project getProject() {
        return project;
    }

    public Lookup getOwnerProjectLookup() {
        return project.getLookup();
    }

    public FileObject getProjectDirectory() {
        return project.getProjectDirectory();
    }

    public File getProjectDirectoryAsFile() {
        return projectDirectoryAsFile;
    }

    public String getName() {
        return currentModel.getMainModule().getShortName();
    }

    public boolean hasEverBeenLoaded() {
        return hasEverBeenLoaded;
    }

    private Lookup getCombinedLookup() {
        Lookup lookup = combinedLookupRef.get();
        if (lookup == null) {
            lookup = new ProxyLookup(
                    getPermanentProjectLookup(),
                    getProjectLookup(),
                    getExtensionLookup());
            combinedLookupRef.compareAndSet(null, lookup);
            lookup = combinedLookupRef.get();
        }
        return lookup;
    }

    private void fireModelChange() {
        for (JavaModelChangeListener listener: getCombinedLookup().lookupAll(JavaModelChangeListener.class)) {
            listener.onModelChange();
        }
        modelChangeListeners.fireEventually();
    }

    private static ProjectInfoManager getProjectInfoManager(Project project) {
        // TODO: In the future this should be a public API.
        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
        if (gradleProject == null) {
            throw new IllegalStateException("project is not an " + NbGradleProject.class.getSimpleName() + ": " + project.getProjectDirectory());
        }

        return gradleProject.getProjectInfoManager();
    }

    private void checkDependencyResolveProblems(NbJavaModule module) {
        String projectName = module.getProperties().getProjectName();

        List<DependencyResolutionIssue> issues = new LinkedList<>();
        for (JavaSourceSet sourceSet: module.getSources()) {
            String sourceSetName = sourceSet.getName();

            Throwable compileProblems = sourceSet.getCompileClassPathProblem();
            if (compileProblems != null) {
                issues.add(DependencyResolutionIssue.compileIssue(projectName, sourceSetName, compileProblems));
            }

            Throwable runtimeProblems = sourceSet.getRuntimeClassPathProblem();
            if (runtimeProblems != null) {
                issues.add(DependencyResolutionIssue.runtimeIssue(projectName, sourceSetName, runtimeProblems));
            }
        }

        if (!issues.isEmpty()) {
            List<ProjectInfo.Entry> entries = new ArrayList<>(issues.size());
            for (DependencyResolutionIssue issue: issues) {
                entries.add(new ProjectInfo.Entry(ProjectInfo.Kind.ERROR, issue.getMessage()));
            }

            dependencyResolutionFailureRef.setInfo(new ProjectInfo(entries));
            ModelLoadIssueReporter.reportDependencyResolutionFailures(issues);
        }
        else {
            dependencyResolutionFailureRef.setInfo(null);
        }
    }

    private void markOwnerIfNecessary(Path projectDir, File dir) {
        if (!dir.toPath().startsWith(projectDir)) {
            URI dirUri = Utilities.toURI(dir);
            FileOwnerQuery.markExternalOwner(dirUri, project, FileOwnerQuery.EXTERNAL_ALGORITHM_TRANSIENT);
        }
    }

    private void markOwnerIfNecessary(Path projectDir, Collection<? extends File> dirs) {
        for (File dir: dirs) {
            markOwnerIfNecessary(projectDir, dir);
        }
    }

    private void markOwnedOutputDirs(Path projectDir, JavaSourceSet sourceSet) {
        JavaOutputDirs outputDirs = sourceSet.getOutputDirs();
        markOwnerIfNecessary(projectDir, outputDirs.getClassesDir());
        markOwnerIfNecessary(projectDir, outputDirs.getResourcesDir());
        markOwnerIfNecessary(projectDir, outputDirs.getOtherDirs());
    }

    private void markOwnedDirs(NbJavaModule mainModule) {
        Path projectDir = getProjectDirectoryAsFile().toPath();
        for (JavaSourceSet sourceSet: mainModule.getSources()) {
            markOwnedOutputDirs(projectDir, sourceSet);

            for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
                markOwnerIfNecessary(projectDir, sourceGroup.getSourceRoots());
            }
        }
    }

    @Override
    public void activateExtension(NbJavaModel parsedModel) {
        ExceptionHelper.checkNotNullArgument(parsedModel, "parsedModel");

        currentModel = parsedModel;
        hasEverBeenLoaded = true;

        NbJavaModule mainModule = parsedModel.getMainModule();

        checkDependencyResolveProblems(mainModule);
        markOwnedDirs(mainModule);

        fireModelChange();
    }

    @Override
    public void deactivateExtension() {
    }

    private static PropertySource<CloseableAction> classPathProviderProperty(
            JavaExtension javaExt,
            String... classPathTypes) {
        ClassPathProviderProperty src = new ClassPathProviderProperty(javaExt, classPathTypes);

        return SwingProperties.fromSwingSource(src, new SwingForwarderFactory<PropertyChangeListener>() {
            @Override
            public PropertyChangeListener createForwarder(final Runnable listener) {
                return new PropertyChangeListener() {
                    @Override
                    public void propertyChange(PropertyChangeEvent evt) {
                        listener.run();
                    }
                };
            }
        });
    }

    private static final class ClassPathProviderProperty
    implements
            SwingPropertySource<CloseableAction, PropertyChangeListener> {

        private final GradleClassPathProvider cpProvider;
        private final CloseableAction pathRegAction;

        public ClassPathProviderProperty(
                JavaExtension javaExt,
                String... classPathTypes) {

            this.cpProvider = javaExt.cpProvider;
            GlobalPathReg[] pathRegs = new GlobalPathReg[classPathTypes.length];
            for (int i = 0; i < classPathTypes.length; i++) {
                pathRegs[i] = new GlobalPathReg(javaExt, classPathTypes[i]);
            }
            this.pathRegAction = CloseableActionContainer.mergeActions(pathRegs);
        }

        @Override
        public CloseableAction getValue() {
            return pathRegAction;
        }

        @Override
        public void addChangeListener(PropertyChangeListener listener) {
            cpProvider.addPropertyChangeListener(listener);
        }

        @Override
        public void removeChangeListener(PropertyChangeListener listener) {
            cpProvider.removePropertyChangeListener(listener);
        }
    }

    // OpenHook is important for debugging because the debugger relies on the
    // globally registered source class paths for source stepping.
    private static class OpenHook extends ProjectOpenedHook {
        private final CloseableActionContainer closeableActions;

        public OpenHook(JavaExtension javaExt) {
            this.closeableActions = new CloseableActionContainer();

            closeableActions.defineAction(classPathProviderProperty(javaExt,
                    ClassPath.SOURCE,
                    ClassPath.BOOT,
                    ClassPath.COMPILE,
                    ClassPath.EXECUTE));
        }

        @Override
        protected void projectOpened() {
            closeableActions.open();
        }

        @Override
        protected void projectClosed() {
            closeableActions.close();
        }
    }

    private static class GlobalPathReg implements CloseableAction {
        private final JavaExtension javaExt;
        private final String type;

        public GlobalPathReg(JavaExtension javaExt, String type) {
            this.javaExt = javaExt;
            this.type = type;
        }

        @Override
        public Ref open() {
            final GlobalPathRegistry registry = GlobalPathRegistry.getDefault();
            final ClassPath[] paths = new ClassPath[]{javaExt.cpProvider.getClassPaths(type)};

            LOGGER.log(Level.FINE,
                    "Registering ClassPath ({0}) for project: {1}",
                    new Object[]{type, javaExt.getProjectDirectoryAsFile()});
            registry.register(type, paths);

            return new Ref() {
                @Override
                public void close() {
                    registry.unregister(type, paths);
                    LOGGER.log(Level.FINE,
                            "Unregistered ClassPath ({0}) for project: {1}",
                            new Object[]{type, javaExt.getProjectDirectoryAsFile()});
                }
            };
        }
    }
}
