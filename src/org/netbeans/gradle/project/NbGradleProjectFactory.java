package org.netbeans.gradle.project;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ProjectFactory;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

@org.openide.util.lookup.ServiceProvider(service = ProjectFactory.class)
public class NbGradleProjectFactory implements ProjectFactory {
    private static final Logger LOGGER = Logger.getLogger(NbGradleProjectFactory.class.getName());

    private static ConcurrentMap<File, Counter> SAFE_TO_OPEN_PROJECTS = new ConcurrentHashMap<File, Counter>();
    private static final String TEMP_DIR = System.getProperty("java.io.tmpdir");

    public static Closeable safeToOpen(FileObject projectDir) {
        File projectDirFile = FileUtil.toFile(projectDir);
        if (projectDirFile == null) {
            return DummyCloseable.INSTANCE;
        }

        while (true) {
            Counter counter = SAFE_TO_OPEN_PROJECTS.get(projectDirFile);
            if (counter != null) {
                counter.increment();
                if (SAFE_TO_OPEN_PROJECTS.get(projectDirFile) == counter) {
                    return new CounterCloser(projectDirFile, counter);
                }
            }
            else {
                counter = new Counter(1);
                if (SAFE_TO_OPEN_PROJECTS.putIfAbsent(projectDirFile, counter) == null) {
                    return new CounterCloser(projectDirFile, counter);
                }
            }
        }
    }

    public static boolean isSafeToOpen(FileObject projectDir) {
        File projectDirFile = FileUtil.toFile(projectDir);
        if (projectDirFile == null) {
            return false;
        }

        return SAFE_TO_OPEN_PROJECTS.containsKey(projectDirFile);
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

        if (projectDirectory.getFileObject(GradleProjectConstants.BUILD_FILE_NAME) != null) {
            return true;
        }
        if (projectDirectory.getFileObject(GradleProjectConstants.SETTINGS_FILE_NAME) != null) {
            return true;
        }
        if (projectDirectory.getFileObject(projectDirectory.getNameExt() + ".gradle") != null) {
            return true;
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
        return new NbGradleProject(dir, state);
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

    private static class Counter {
        private final AtomicLong counter;

        public Counter(long initialValue) {
            this.counter = new AtomicLong(initialValue);
        }

        public void increment() {
            counter.incrementAndGet();
        }

        public boolean decrement() {
            return counter.decrementAndGet() <= 0;
        }
    }

    private static class CounterCloser implements Closeable {
        private final File key;
        private final AtomicReference<Counter> counterRef;

        public CounterCloser(File key, Counter counter) {
            if (key == null) throw new NullPointerException("key");
            if (counter == null) throw new NullPointerException("counter");
            this.key = key;
            this.counterRef = new AtomicReference<Counter>(counter);
        }

        @Override
        public void close() {
            Counter counter = counterRef.getAndSet(null);
            if (counter != null) {
                if (counter.decrement()) {
                    SAFE_TO_OPEN_PROJECTS.remove(key, counter);
                    LOGGER.log(Level.INFO, "Project is not safe to load anymore: {0}", key);
                }
            }
        }
    }
}