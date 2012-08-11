package org.netbeans.gradle.project;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.Collections;
import java.util.EnumMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.ExternalDependency;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.Model;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaDependency;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaModuleDependency;
import org.gradle.tooling.model.idea.IdeaProject;
import org.gradle.tooling.model.idea.IdeaSourceDirectory;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.model.NbDependencyGroup;
import org.netbeans.gradle.project.model.NbDependencyType;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleModule;
import org.netbeans.gradle.project.model.NbModuleDependency;
import org.netbeans.gradle.project.model.NbOutput;
import org.netbeans.gradle.project.model.NbSourceGroup;
import org.netbeans.gradle.project.model.NbSourceType;
import org.netbeans.gradle.project.model.NbUriDependency;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.Lookup;
import org.openide.util.NbBundle;
import org.openide.util.RequestProcessor;
import org.openide.util.Utilities;
import org.openide.util.lookup.Lookups;

public final class NbGradleProject implements Project {
    private static final Logger LOGGER = Logger.getLogger(NbGradleProject.class.getName());

    public static final RequestProcessor PROJECT_PROCESSOR
            = new RequestProcessor("Gradle-Project-Processor", 1, true);

    public static final RequestProcessor TASK_EXECUTOR
            = new RequestProcessor("Gradle-Task-Executor", 1, true);

    public static final RequestProcessor PROJECT_LOADER
            = new RequestProcessor("Gradle-Project-Loader", 1, true);

    private final FileObject projectDir;
    private final ProjectState state;
    private final AtomicReference<Lookup> lookupRef;

    private final GradleClassPathProvider cpProvider;

    private final ChangeSupport modelChanges;
    private final AtomicBoolean hasModelBeenLoaded;
    private volatile NbGradleModel currentModel;

    public NbGradleProject(FileObject projectDir, ProjectState state) throws IOException {
        this.projectDir = projectDir;
        this.state = state;
        this.lookupRef = new AtomicReference<Lookup>(null);
        this.cpProvider = new GradleClassPathProvider(this);

        this.hasModelBeenLoaded = new AtomicBoolean(false);
        this.modelChanges = new ChangeSupport(this);
        this.currentModel = createEmptyModel(projectDir);
    }

    public void addModelChangeListener(ChangeListener listener) {
        modelChanges.addChangeListener(listener);
    }

    public void removeModelChangeListener(ChangeListener listener) {
        modelChanges.removeChangeListener(listener);
    }

    public NbGradleModel getCurrentModel() {
        loadProject(true);
        return currentModel;
    }

    public void reloadProject() {
        loadProject(false);
    }

    private void loadProject(boolean onlyIfNotLoaded) {
        if (!hasModelBeenLoaded.compareAndSet(false, true)) {
            if (onlyIfNotLoaded) {
                return;
            }
        }

        PROJECT_LOADER.execute(new Runnable() {
            @Override
            public void run() {
                try {
                    currentModel = loadModel(projectDir);
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Failed to load the project.", ex);
                }

                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        modelChanges.fireChange();
                    }
                });
            }
        });
    }

    private static NbOutput createDefaultOutput(File projectDir) {
        File buildDir = new File(projectDir, "build" + File.pathSeparator + "classes");

        return new NbOutput(
                new File(buildDir, "main"),
                new File(buildDir, "test"));
    }

    private static NbGradleModel createEmptyModel(FileObject projectDir) throws IOException {
        File projectDirAsFile = FileUtil.toFile(projectDir);
        String name = projectDir.getNameExt();
        NbGradleModule.Properties properties = new NbGradleModule.Properties(
                name,
                projectDirAsFile,
                createDefaultOutput(projectDirAsFile),
                Collections.<String>emptyList());

        NbGradleModule mainModule = new NbGradleModule(properties,
                Collections.<NbSourceType, NbSourceGroup>emptyMap(),
                Collections.<NbDependencyType, NbDependencyGroup>emptyMap());

        return new NbGradleModel(projectDir, mainModule);
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

    private static File tryGetModuleDir(IdeaModule module) {
        DomainObjectSet<? extends IdeaContentRoot> contentRoots = module.getContentRoots();
        return contentRoots.isEmpty() ? null : contentRoots.getAt(0).getRootDirectory();
    }

    private static IdeaModule tryFindMainModule(FileObject projectDir, IdeaProject ideaModel) {
        File projectDirAsFile = FileUtil.toFile(projectDir);
        for (IdeaModule module: ideaModel.getModules()) {
            File moduleDir = tryGetModuleDir(module);
            if (moduleDir != null && moduleDir.equals(projectDirAsFile)) {
                return module;
            }
        }
        return null;
    }

    private static Map<NbDependencyType, NbDependencyGroup> getDependencies(
            IdeaModule module, Set<String> parsedModules) {

        DependencyBuilder dependencies = new DependencyBuilder();

        for (IdeaDependency dependency: module.getDependencies()) {
            String scope = dependency.getScope().getScope();
            NbDependencyType dependencyType;
            if ("COMPILE".equals(scope)) {
                dependencyType = NbDependencyType.COMPILE;
            }
            else if ("TEST".equals(scope)) {
                dependencyType = NbDependencyType.TEST_COMPILE;
            }
            else if ("RUNTIME".equals(scope)) {
                dependencyType = NbDependencyType.RUNTIME;
            }
            else {
                dependencyType = NbDependencyType.OTHER;
            }

            if (dependency instanceof IdeaModuleDependency) {
                IdeaModuleDependency moduleDep = (IdeaModuleDependency)dependency;

                NbGradleModule parsedDependency = tryParseModule(moduleDep.getDependencyModule(), parsedModules);
                if (parsedDependency != null) {
                    dependencies.addModuleDependency(
                            dependencyType,
                            new NbModuleDependency(parsedDependency, true));
                }
            }
            else if (dependency instanceof ExternalDependency) {
                ExternalDependency externalDep = (ExternalDependency)dependency;
                URI uri = Utilities.toURI(externalDep.getFile());

                dependencies.addUriDependency(
                        dependencyType,
                        new NbUriDependency(uri, true));
            }
            else {
                LOGGER.log(Level.WARNING, "Unknown dependency: {0}", dependency);
            }
        }
        Map<NbDependencyType, NbDependencyGroup> dependencyMap
                = new EnumMap<NbDependencyType, NbDependencyGroup>(NbDependencyType.class);
        for (NbDependencyType type: NbDependencyType.values()) {
            NbDependencyGroup group = dependencies.getGroup(type);
            if (!group.isEmpty()) {
                dependencyMap.put(type, group);
            }
        }
        return dependencyMap;
    }

    private static boolean isResourcePath(IdeaSourceDirectory srcDir) {
        return srcDir.getDirectory().getName().toLowerCase(Locale.US).startsWith("resource");
    }

    private static Map<NbSourceType, NbSourceGroup> getSources(IdeaModule module) {
        List<File> sources = new LinkedList<File>();
        List<File> resources = new LinkedList<File>();
        List<File> testSources = new LinkedList<File>();
        List<File> testResources = new LinkedList<File>();

        for (IdeaContentRoot contentRoot: module.getContentRoots()) {
            for (IdeaSourceDirectory ideaSrcDir: contentRoot.getSourceDirectories()) {
                if (isResourcePath(ideaSrcDir)) {
                    resources.add(ideaSrcDir.getDirectory());
                }
                else {
                    sources.add(ideaSrcDir.getDirectory());
                }
            }
            for (IdeaSourceDirectory ideaTestDir: contentRoot.getTestDirectories()) {
                if (isResourcePath(ideaTestDir)) {
                    testResources.add(ideaTestDir.getDirectory());
                }
                else {
                    testSources.add(ideaTestDir.getDirectory());
                }
            }
        }

        Map<NbSourceType, NbSourceGroup> groups = new EnumMap<NbSourceType, NbSourceGroup>(NbSourceType.class);
        if (!sources.isEmpty()) {
            groups.put(NbSourceType.SOURCE, new NbSourceGroup(sources));
        }
        if (!resources.isEmpty()) {
            groups.put(NbSourceType.RESOURCE, new NbSourceGroup(resources));
        }
        if (!testSources.isEmpty()) {
            groups.put(NbSourceType.TEST_SOURCE, new NbSourceGroup(testSources));
        }
        if (!testResources.isEmpty()) {
            groups.put(NbSourceType.TEST_RESOURCE, new NbSourceGroup(testResources));
        }
        return groups;
    }

    private static NbGradleModule tryParseModule(IdeaModule module, Set<String> parsedModules) {
        String uniqueName = module.getGradleProject().getPath();
        if (parsedModules.contains(uniqueName)) {
            LOGGER.log(Level.WARNING, "Circular dependency: {0}", uniqueName);
            return null;
        }
        parsedModules.add(uniqueName);

        Map<NbDependencyType, NbDependencyGroup> dependencies
                = getDependencies(module, parsedModules);

        Map<NbSourceType, NbSourceGroup> sources = getSources(module);

        File moduleDir = tryGetModuleDir(module);
        if (moduleDir == null) {
            LOGGER.log(Level.WARNING, "Unable to find the project directory: {0}", uniqueName);
            return null;
        }

        List<String> taskNames = new LinkedList<String>();
        for (GradleTask task: module.getGradleProject().getTasks()) {
            taskNames.add(task.getName());
        }

        NbGradleModule.Properties properties = new NbGradleModule.Properties(
                uniqueName,
                moduleDir,
                createDefaultOutput(moduleDir),
                taskNames);

        return new NbGradleModule(properties, sources, dependencies);
    }

    private static NbGradleModel parseFromIdeaModel(
            FileObject projectDir, IdeaProject ideaModel) throws IOException {
        IdeaModule mainModule = tryFindMainModule(projectDir, ideaModel);
        if (mainModule == null) {
            throw new IOException("Unable to find the main project in the model.");
        }

        NbGradleModule parsedMainModule = tryParseModule(mainModule, new HashSet<String>());
        if (parsedMainModule == null) {
            throw new IOException("Unable to parse the main project from the model.");
        }
        return new NbGradleModel(projectDir, parsedMainModule);
    }

    private static NbGradleModel loadModelWithProgress(
            FileObject projectDir,
            ProgressHandle progress) throws IOException {

        IdeaProject ideaModel;

        GradleConnector gradleConnector = GradleConnector.newConnector();
        gradleConnector.forProjectDirectory(FileUtil.toFile(projectDir));
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            ideaModel = getModelWithProgress(progress, projectConnection, IdeaProject.class);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }

        return parseFromIdeaModel(projectDir, ideaModel);
    }

    private static NbGradleModel loadModel(FileObject projectDir) throws IOException {
        LOGGER.log(Level.INFO, "Loading Gradle project from directory: {0}", projectDir);

        ProgressHandle progress = ProgressHandleFactory.createHandle(
                NbBundle.getMessage(NbGradleProject.class, "LBL_LoadingProject", projectDir.getNameExt()));
        try {
            progress.start();
            return loadModelWithProgress(projectDir, progress);
        } finally {
            progress.finish();
        }
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
            reloadProject();

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

    private static class DependencyBuilder {
        private final Map<NbDependencyType, List<NbModuleDependency>> moduleDependencies;
        private final Map<NbDependencyType, List<NbUriDependency>> uriDependencies;

        public DependencyBuilder() {
            this.moduleDependencies = new EnumMap<NbDependencyType, List<NbModuleDependency>>(NbDependencyType.class);
            this.uriDependencies = new EnumMap<NbDependencyType, List<NbUriDependency>>(NbDependencyType.class);
        }

        public static <T> void addDependency(
                NbDependencyType type,
                T dependency,
                Map<NbDependencyType, List<T>> storage) {
            List<T> list = storage.get(type);
            if (list == null) {
                list = new LinkedList<T>();
                storage.put(type, list);
            }
            list.add(dependency);
        }

        public void addModuleDependency(NbDependencyType type, NbModuleDependency dependency) {
            addDependency(type, dependency, moduleDependencies);
        }

        public void addUriDependency(NbDependencyType type, NbUriDependency dependency) {
            addDependency(type, dependency, uriDependencies);
        }

        private static <T> List<T> getDependencies(
                NbDependencyType type,
                Map<NbDependencyType, List<T>> storage) {
            List<T> dependencies = storage.get(type);
            return dependencies != null ? dependencies : Collections.<T>emptyList();
        }

        public NbDependencyGroup getGroup(NbDependencyType type) {
            return new NbDependencyGroup(
                    getDependencies(type, moduleDependencies),
                    getDependencies(type, uriDependencies));
        }
    }
}
