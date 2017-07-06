package org.netbeans.gradle.project.util;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.TimeUnit;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.WaitableSignal;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.SyncTaskExecutor;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class FileSystemWatcherTest {
    private static final long TIMEOUT_SEC = 5;

    @Rule
    public final TemporaryFolder tmpFolder = new TemporaryFolder();

    private FileSystemWatcher watcher;

    @Before
    public void setupTest() {
        watcher = new FileSystemWatcher(FileSystems.getDefault(), SyncTaskExecutor.getSimpleExecutor());
    }

    private void testModifications(WatchSetup setup, Modification... modifications) throws IOException {
        Path root = tmpFolder.newFolder("root").toPath();

        TestListener listener = new TestListener();

        Path watchedDir = setup.setupWatch(root);

        ListenerRef listenerRef = watcher.watchPath(watchedDir, listener);
        try {
            for (Modification modification: modifications) {
                listener.reset();
                modification.doModification(watchedDir);
                listener.assertCalled();
            }
        } finally {
            listenerRef.unregister();
        }

        watcher.waitFor(TIMEOUT_SEC, TimeUnit.SECONDS);
    }

    @Test
    public void testMultiActionLevel1() throws IOException {
        testModifications(
                root -> root.resolve("subdir"),
                Files::createDirectory,
                Files::delete,
                Files::createDirectory);
    }

    @Test
    public void testCreateLevel1() throws IOException {
        testModifications(
                root -> root.resolve("subdir"),
                Files::createDirectory);
    }

    @Test
    public void testDeleteLevel1() throws IOException {
        testModifications((Path root) -> {
            Path watchedDir = root.resolve("subdir");
            Files.createDirectory(watchedDir);
            return watchedDir;
        }, Files::delete);
    }

    @Test
    public void testCreateLevel2() throws IOException {
        testModifications(
                root -> root.resolve("subdir").resolve("subdir2"),
                Files::createDirectories);
    }

    @Test
    public void testDeleteLevel2() throws IOException {
        testModifications((Path root) -> {
            Path watchedDir = root.resolve("subdir").resolve("subdir2");
            Files.createDirectories(watchedDir);
            return watchedDir;
        }, Files::delete);
    }

    private static final class TestListener implements Runnable {
        private WaitableSignal signal;

        public TestListener() {
            this.signal = new WaitableSignal();
        }

        public void reset() {
            signal = new WaitableSignal();
        }

        @Override
        public void run() {
            signal.signal();
        }

        public void assertCalled() {
            if (!signal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, TIMEOUT_SEC, TimeUnit.SECONDS)) {
                throw new AssertionError("waitCalled: Timeout");
            }
        }
    }

    private interface WatchSetup {
        public Path setupWatch(Path root) throws IOException;
    }

    private interface Modification {
        public void doModification(Path watchedDir) throws IOException;
    }
}
