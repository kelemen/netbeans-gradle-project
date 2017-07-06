package org.netbeans.gradle.project;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.project.model.NbGenericModelInfo;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.DefaultScriptFileProvider;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectFactory2;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

@org.openide.util.lookup.ServiceProvider(service = ProjectFactory.class)
public class NbGradleProjectFactory implements ProjectFactory2 {
    private static final Logger LOGGER = Logger.getLogger(NbGradleProjectFactory.class.getName());

    static final RootProjectRegistry ROOT_PROJECT_REGISTRY = new RootProjectRegistry();
    static final GlobalSettingsFileManager SETTINGS_FILE_MANAGER
            = new DefaultGlobalSettingsFileManager(ROOT_PROJECT_REGISTRY);

    public static final ScriptFileProvider DEFAULT_SCRIPT_FILE_PROVIDER = new DefaultScriptFileProvider();

    private static final ConcurrentMap<Path, RefCounter> SAFE_TO_OPEN_PROJECTS
            = new ConcurrentHashMap<>();

    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    public static Project loadSafeProject(Path projectDir) throws IOException {
        return loadSafeProject(projectDir.toFile());
    }

    public static Project loadSafeProject(File projectDir) throws IOException {
        FileObject projectDirObj = FileUtil.toFileObject(projectDir);
        if (projectDirObj == null) {
            throw new IllegalArgumentException("Project directory does not exist: " + projectDir);
        }
        return loadSafeProject(projectDirObj);
    }

    public static Project loadSafeProject(FileObject projectDir) throws IOException {
        Project result = findSafeProject(projectDir);
        if (result == null) {
            throw new IllegalArgumentException("Project does not exist: " + projectDir);
        }
        return result;
    }

    public static NbGradleProject tryGetGradleProject(Project project) {
        return project != null
                ? project.getLookup().lookup(NbGradleProject.class)
                : null;
    }

    public static NbGradleProject getGradleProject(Project project) {
        NbGradleProject gradleProject = tryGetGradleProject(project);
        if (gradleProject == null) {
            throw new IllegalArgumentException("Not a Gradle project: " + project.getProjectDirectory());
        }
        return gradleProject;
    }

    public static NbGradleProject tryLoadSafeGradleProject(Path projectDir) {
        return tryGetGradleProject(tryLoadSafeProject(projectDir));
    }

    public static NbGradleProject tryLoadSafeGradleProject(File projectDir) {
        return tryGetGradleProject(tryLoadSafeProject(projectDir));
    }

    public static Project tryLoadSafeProject(Path projectDir) {
        return tryLoadSafeProject(projectDir.toFile());
    }

    public static Project tryLoadSafeProject(File projectDir) {
        Objects.requireNonNull(projectDir, "projectDir");

        FileObject projectDirObj = FileUtil.toFileObject(projectDir);
        if (projectDirObj == null) {
            return null;
        }
        return tryLoadSafeProject(projectDirObj);
    }

    public static Project tryLoadSafeProject(FileObject projectDir) {
        try {
            return findSafeProject(projectDir);
        } catch (IOException | IllegalArgumentException ex) {
            LOGGER.log(Level.INFO, "Failed to load project: " + projectDir, ex);
            return null;
        }
    }

    private static Project findSafeProject(FileObject projectDir) throws IOException {
        Objects.requireNonNull(projectDir, "projectDir");

        try (Closeable safeToOpen = NbGradleProjectFactory.safeToOpen(projectDir)) {
            assert safeToOpen != null; // Avoid warning

            return ProjectManager.getDefault().findProject(projectDir);
        }
    }

    public static Closeable safeToOpen(FileObject projectDir) {
        File projectDirFile = FileUtil.toFile(projectDir);
        if (projectDirFile == null) {
            return DummyCloseable.INSTANCE;
        }

        return safeToOpen(projectDirFile);
    }

    public static Closeable safeToOpen(File projectDir) {
        return safeToOpen(projectDir.toPath());
    }

    public static Closeable safeToOpen(Path projectDir) {
        Objects.requireNonNull(projectDir, "projectDir");

        Path normProjectDir = projectDir.normalize();

        RefCounter counter;
        RefCounter newCounter;
        boolean set;

        do {
            counter = SAFE_TO_OPEN_PROJECTS.get(normProjectDir);
            if (counter == null) {
                newCounter = new RefCounter(1);
                set = SAFE_TO_OPEN_PROJECTS.putIfAbsent(normProjectDir, newCounter) == null;
            }
            else {
                newCounter = counter.increment();
                set = SAFE_TO_OPEN_PROJECTS.replace(normProjectDir, counter, newCounter);
            }
        } while(!set);

        if (counter == null) {
            LOGGER.log(Level.INFO, "Project is now safe to load: {0}", projectDir);
        }

        return new CounterCloser(normProjectDir);
    }

    public static boolean isSafeToOpen(FileObject projectDir) {
        Path projectDirPath = NbFileUtils.asPath(FileUtil.toFile(projectDir));
        if (projectDirPath == null) {
            return false;
        }

        return SAFE_TO_OPEN_PROJECTS.containsKey(projectDirPath);
    }

    private static boolean hasBuildFile(FileObject directory) {
        Path dirAsPath = NbFileUtils.asPath(directory);
        return dirAsPath != null
                ? NbGenericModelInfo.tryGuessBuildFilePath(dirAsPath, DEFAULT_SCRIPT_FILE_PROVIDER) != null
                : false;
    }

    @Override
    public ProjectManager.Result isProject2(FileObject projectDirectory) {
        if (isProject(projectDirectory)) {
            return new ProjectManager.Result(NbIcons.getGradleIconAsIcon());
        }
        else {
            return null;
        }
    }

    @Override
    public boolean isProject(FileObject projectDirectory) {
        if (isSafeToOpen(projectDirectory)) {
            return true;
        }

        // We will not load projects from the temporary directory simply
        // because NetBeans has a habit to put temporary gradle files to
        // them and then tries to load it which will fail because NetBeans will
        // delete them soon.
        if (TEMP_DIR != null) {
            File tempDir = FileUtil.normalizeFile(new File(TEMP_DIR));
            FileObject tempDirObj = FileUtil.toFileObject(tempDir);
            if (tempDirObj != null) {
                if (FileUtil.getRelativePath(tempDirObj, projectDirectory) != null) {
                    return false;
                }
            }
        }

        if (hasBuildFile(projectDirectory)) {
            return true;
        }
        if (projectDirectory.getNameExt().equalsIgnoreCase(CommonScripts.BUILD_SRC_NAME)) {
            FileObject parent = projectDirectory.getParent();
            if (parent != null) {
                if (hasBuildFile(parent)) {
                    return true;
                }
            }
        }

        return false;
    }

    @Override
    public Project loadProject(FileObject dir, ProjectState state) throws IOException {
        // Note: Netbeans might call this method without calling isProject
        //  first on directories within the project. If this method throws
        //  an exception in this case, NetBeans will fail to check for the class
        //  path of files. And finding the cause of such behaviour is extremly
        //  hard if this is not known.
        if (!isProject(dir)) {
            return null;
        }
        return NbGradleProject.createProject(dir, state);
    }

    @Override
    public void saveProject(final Project project) throws IOException {
    }

    private enum DummyCloseable implements Closeable {
        INSTANCE;

        @Override
        public void close() {
        }

    }

    private static class RefCounter {
        private final long refCount;

        public RefCounter(long value) {
            this.refCount = value;
        }

        public long getRefCount() {
            return refCount;
        }

        public RefCounter increment() {
            return new RefCounter(refCount + 1);
        }

        public RefCounter decrement() {
            return new RefCounter(refCount - 1);
        }
    }

    private static class CounterCloser implements Closeable {
        private final Path key;
        private final AtomicBoolean closed;

        public CounterCloser(Path key) {
            this.key = Objects.requireNonNull(key, "key");
            this.closed = new AtomicBoolean(false);
        }

        @Override
        public void close() {
            if (closed.compareAndSet(false, true)) {
                RefCounter counter;
                RefCounter newCounter;
                boolean set;
                do {
                    counter = SAFE_TO_OPEN_PROJECTS.get(key);
                    newCounter = counter.decrement();

                    if (newCounter.getRefCount() == 0) {
                        set = SAFE_TO_OPEN_PROJECTS.remove(key, counter);
                    }
                    else {
                        set = SAFE_TO_OPEN_PROJECTS.replace(key, counter, newCounter);
                    }
                } while (!set);

                if (newCounter.getRefCount() == 0) {
                    LOGGER.log(Level.INFO, "Project is not safe to load anymore: {0}", key);
                }
            }
        }
    }
}