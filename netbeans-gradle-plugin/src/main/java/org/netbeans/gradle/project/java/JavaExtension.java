package org.netbeans.gradle.project.java;

import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingProperties;
import org.jtrim2.property.swing.SwingPropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.jtrim2.utils.LazyValues;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.ProjectIssue;
import org.netbeans.gradle.project.ProjectIssueManager;
import org.netbeans.gradle.project.ProjectIssueRef;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.coverage.GradleCoverageProvider;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.java.model.JavaParsingUtils;
import org.netbeans.gradle.project.java.model.JavaProjectDependencies;
import org.netbeans.gradle.project.java.model.JavaSourceDirHandler;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.nodes.JavaExtensionNodes;
import org.netbeans.gradle.project.java.nodes.JavaProjectContextActions;
import org.netbeans.gradle.project.java.properties.JavaDebuggingPanel;
import org.netbeans.gradle.project.java.properties.JavaModulesPanel;
import org.netbeans.gradle.project.java.properties.JavaProjectProperties;
import org.netbeans.gradle.project.java.query.GradleAnnotationProcessingQuery;
import org.netbeans.gradle.project.java.query.GradleBinaryForSourceQuery;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.java.query.GradleCompilerOptionsQuery;
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
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.script.ScriptFileProvider;
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
    private final MutableProperty<NbJavaModel> currentModel;
    private volatile boolean hasEverBeenLoaded;

    private final GradleClassPathProvider cpProvider;
    private final Supplier<JavaSourceDirHandler> sourceDirsHandlerRef;
    private final ProjectIssueRef dependencyResolutionFailureRef;
    private final JavaProjectDependencies projectDependencies;

    private final Supplier<Lookup> projectLookupRef;
    private final Supplier<Lookup> permanentLookupRef;
    private final Supplier<Lookup> extensionLookupRef;
    private final Supplier<Lookup> combinedLookupRef;

    private final ChangeListenerManager modelChangeListeners;
    private final Supplier<JavaProjectProperties> projectPropertiesRef;
    private final Supplier<ProjectSettingsProvider.ExtensionSettings> extensionSettingsRef;

    private JavaExtension(Project project) throws IOException {
        this.project = Objects.requireNonNull(project, "project");

        this.projectDirectoryAsFile = FileUtil.toFile(project.getProjectDirectory());
        if (projectDirectoryAsFile == null) {
            throw new IOException("Project directory does not exist: " + project.getProjectDirectory());
        }

        NbJavaModel defaultModel = JavaParsingUtils.createEmptyModel(
                project.getProjectDirectory(),
                project.getLookup().lookup(ScriptFileProvider.class));

        this.currentModel = PropertyFactory.memPropertyConcurrent(defaultModel, SwingExecutors.getStrictExecutor(false));
        this.cpProvider = new GradleClassPathProvider(this);
        this.projectDependencies = new JavaProjectDependencies(this);
        this.hasEverBeenLoaded = false;

        this.dependencyResolutionFailureRef = getProjectInfoManager(project).createIssueRef();
        this.modelChangeListeners = new GenericChangeListenerManager();

        this.projectLookupRef = LazyValues.lazyValue(() -> {
            return Lookups.fixed(
                    LookupProviderSupport.createSourcesMerger(),
                    new GradleProjectSources(this),
                    cpProvider,
                    new GradleCoverageProvider(this),
                    new GradleSourceLevelQueryImplementation(this),
                    new GradleUnitTestFinder(this),
                    new GradleAnnotationProcessingQuery(),
                    new GradleSourceForBinaryQuery(this),
                    new GradleBinaryForSourceQuery(this),
                    new GradleProjectTemplates(),
                    new JavaGradleTaskVariableQuery(this),
                    new GradleCompilerOptionsQuery(this),
                    new J2SEPlatformFromScriptQueryImpl(this) // internal use only
            );
        });
        this.permanentLookupRef = LazyValues.lazyValue(() -> {
            return Lookups.fixed(
                    this,
                    new OpenHook(this));
        });
        this.extensionLookupRef = LazyValues.lazyValue(() -> {
            return Lookups.fixed(
                    new JavaExtensionNodes(this),
                    new JavaProjectContextActions(this),
                    new GradleJavaBuiltInCommands(this),
                    new JavaInitScriptQuery(),
                    JavaModulesPanel.createCustomizer(true),
                    JavaDebuggingPanel.createDebuggingCustomizer(true));
        });
        this.combinedLookupRef = LazyValues.lazyValue(() -> {
            return new ProxyLookup(
                    getPermanentProjectLookup(),
                    getProjectLookup(),
                    getExtensionLookup());
        });
        this.sourceDirsHandlerRef = LazyValues.lazyValue(() -> new JavaSourceDirHandler(this));
        this.projectPropertiesRef = LazyValues.lazyValue(() -> {
            ProjectSettingsProvider.ExtensionSettings extensionSettings = getExtensionSettings();
            return new JavaProjectProperties(extensionSettings.getActiveSettings());
        });
        this.extensionSettingsRef = LazyValues.lazyValue(() -> {
            ProjectSettingsProvider settingsProvider = project.getLookup().lookup(ProjectSettingsProvider.class);
            if (settingsProvider == null) {
                throw new IllegalArgumentException("Not a Gradle project.");
            }
            return settingsProvider.getExtensionSettings(JavaExtensionDef.EXTENSION_NAME);
        });
    }

    public static PropertySource<JavaExtension> extensionOfProject(Project project) {
        Objects.requireNonNull(project, "project");

        PropertySource<JavaExtension> extRef = NbProperties.lookupProperty(project.getLookup(), JavaExtension.class);
        return NbProperties.cacheFirstNonNull(extRef);
    }

    public static PropertySource<NbJavaModel> javaModelOfProject(Project project) {
        PropertySource<JavaExtension> extRef = extensionOfProject(project);

        return PropertyFactory.propertyOfProperty(extRef, (JavaExtension ext) -> {
            return ext != null
                    ? ext.currentModel
                    : PropertyFactory.constSource(null);
        });
    }

    public ProjectSettingsProvider.ExtensionSettings getExtensionSettings() {
        return extensionSettingsRef.get();
    }

    public JavaProjectProperties getProjectProperties() {
        return projectPropertiesRef.get();
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
        return sourceDirsHandlerRef.get();
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

    public JavaProjectDependencies getProjectDependencies() {
        return projectDependencies;
    }

    public PropertySource<NbJavaModel> currentModel() {
        return currentModel;
    }

    public NbJavaModel getCurrentModel() {
        return currentModel().getValue();
    }

    private void initLookup(Lookup lookup) {
        for (ProjectInitListener listener: lookup.lookupAll(ProjectInitListener.class)) {
            listener.onInitProject();
        }
    }

    // These classes are on the lookup always.
    @Override
    public Lookup getPermanentProjectLookup() {
        return permanentLookupRef.get();
    }

    @Override
    public Lookup getProjectLookup() {
        return projectLookupRef.get();
    }

    @Override
    public Lookup getExtensionLookup() {
        return extensionLookupRef.get();
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
        return getCurrentModel().getMainModule().getShortName();
    }

    public boolean hasEverBeenLoaded() {
        return hasEverBeenLoaded;
    }

    private Lookup getCombinedLookup() {
        return combinedLookupRef.get();
    }

    private void fireModelChange() {
        for (JavaModelChangeListener listener: getCombinedLookup().lookupAll(JavaModelChangeListener.class)) {
            listener.onModelChange();
        }
        projectDependencies.updateDependencies();
        modelChangeListeners.fireEventually();
    }

    private static ProjectIssueManager getProjectInfoManager(Project project) {
        // TODO: In the future this should be a public API.
        return NbGradleProjectFactory.getGradleProject(project).getProjectIssueManager();
    }

    private void checkDependencyResolveProblems(NbJavaModule module) {
        String projectName = module.getProperties().getProjectName();

        List<DependencyResolutionIssue> issues = new ArrayList<>();
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
            List<ProjectIssue.Entry> entries = new ArrayList<>(issues.size());
            for (DependencyResolutionIssue issue: issues) {
                entries.add(new ProjectIssue.Entry(
                        ProjectIssue.Kind.ERROR,
                        issue.getMessage(),
                        getIssueDescription(issue)));
            }

            dependencyResolutionFailureRef.setInfo(new ProjectIssue(entries));
            ModelLoadIssueReporter.reportDependencyResolutionFailures(issues);
        }
        else {
            dependencyResolutionFailureRef.setInfo(null);
        }
    }

    private static String getIssueDescription(DependencyResolutionIssue issue) {
        StringBuilder result = new StringBuilder(1024);
        result.append(issue.getMessage());
        result.append("\n");

        // TODO: I18N

        result.append("Project: ");
        result.append(issue.getProjectName());
        result.append("\n");

        result.append("Source set: ");
        result.append(issue.getSourceSetName());
        result.append("\n");

        result.append("Dependecy type: ");
        result.append(issue.getDependencyKind().toString());
        result.append("\n");

        result.append("Exception messages: ");
        for (String message: getExceptionMessages(issue.getStackTrace())) {
            result.append("\n  - ");
            result.append(message);
        }

        return result.toString();
    }

    private static List<String> getExceptionMessages(Throwable ex) {
        List<String> result = new ArrayList<>();

        Throwable current = ex;
        while (current != null) {
            for (Throwable suppressed: current.getSuppressed()) {
                result.add(suppressed.getMessage());
            }
            result.add(current.getMessage());

            current = current.getCause();
        }

        Collections.reverse(result);
        return result;
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
        markOwnerIfNecessary(projectDir, outputDirs.getClassesDirs());
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
        Objects.requireNonNull(parsedModel, "parsedModel");

        currentModel.setValue(parsedModel);
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

        return SwingProperties.fromSwingSource(src, (Runnable listener) -> {
            return evt -> listener.run();
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

            return () -> {
                registry.unregister(type, paths);
                LOGGER.log(Level.FINE,
                        "Unregistered ClassPath ({0}) for project: {1}",
                        new Object[]{type, javaExt.getProjectDirectoryAsFile()});
            };
        }
    }
}
