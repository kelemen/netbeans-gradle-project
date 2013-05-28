package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = SourceForBinaryQueryImplementation2.class),
    @ServiceProvider(service = SourceForBinaryQueryImplementation.class)})
public final class GradleCacheSourceForBinaryQuery implements SourceForBinaryQueryImplementation2 {
    private static final FileObject[] NO_ROOTS = new FileObject[0];
    private static final ChangeSupport CHANGES;

    static {
        EventSource eventSource = new EventSource();
        CHANGES = new ChangeSupport(eventSource);
        eventSource.init(CHANGES);
    }

    // This cache cannot shrink because SourceForBinaryQueryImplementation
    // requires that we return the exact same object when the same URL is
    // querried. This is a very limiting constraint but I don't want to risk to
    // violate the constraint.
    private final ConcurrentMap<FileObject, Result> cache;

    public GradleCacheSourceForBinaryQuery() {
        this.cache = new ConcurrentHashMap<FileObject, Result>();
    }

    public static void notifyCacheChange() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                CHANGES.fireChange();
            }
        });
    }

    @Override
    public Result findSourceRoots2(URL binaryRoot) {
        File gradleUserHome = GradleFileUtils.GRADLE_USER_HOME.getValue();
        if (gradleUserHome == null) {
            return null;
        }

        File binaryRootFile = FileUtil.archiveOrDirForURL(binaryRoot);
        if (binaryRootFile == null) {
            return null;
        }
        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRootFile);
        if (binaryRootObj == null) {
            return null;
        }

        Result result = cache.get(binaryRootObj);
        if (result != null) {
            return result;
        }

        FileObject cacheHome = FileUtil.toFileObject(gradleUserHome);
        if (cacheHome == null || !FileUtil.isParentOf(cacheHome, binaryRootObj)) {
            return null;
        }

        FileObject hashDir = binaryRootObj.getParent();
        if (hashDir == null) {
            return null;
        }

        FileObject binDir = hashDir.getParent();
        if (binDir == null) {
            return null;
        }

        if (!GradleFileUtils.canBeBinaryDirName(binDir.getNameExt())) {
            return null;
        }

        final FileObject artifactRoot = binDir.getParent();
        if (artifactRoot == null) {
            return null;
        }

        result = new Result() {
            @Override
            public boolean preferSources() {
                return false;
            }

            @Override
            public FileObject[] getRoots() {
                // The cache directory of Gradle looks like this:
                //
                // ...... \\source\\HASH_OF_SOURCE\\binary-sources.jar
                // ...... \\packaging type\\HASH_OF_BINARY\\binary.jar
                FileObject srcDir = artifactRoot.getFileObject(GradleFileUtils.SOURCE_DIR_NAME);
                if (srcDir == null) {
                    return NO_ROOTS;
                }

                String sourceFileName = GradleFileUtils.binaryToSourceName(binaryRootObj);

                FileObject srcFile = GradleFileUtils.getFileFromASubDir(srcDir, sourceFileName);
                return srcFile != null ? new FileObject[]{srcFile} : NO_ROOTS;
            }

            @Override
            public void addChangeListener(ChangeListener l) {
                CHANGES.addChangeListener(l);
            }

            @Override
            public void removeChangeListener(ChangeListener l) {
                CHANGES.removeChangeListener(l);
            }
        };

        Result oldResult = cache.putIfAbsent(binaryRootObj, result);
        return oldResult != null ? oldResult : result;
    }

    @Override
    public SourceForBinaryQuery.Result findSourceRoots(URL binaryRoot) {
        return findSourceRoots2(binaryRoot);
    }

    private static final class EventSource implements Result {
        private volatile ChangeSupport changes;

        public void init(ChangeSupport changes) {
            assert changes != null;
            this.changes = changes;
        }

        @Override
        public boolean preferSources() {
            return false;
        }

        @Override
        public FileObject[] getRoots() {
            return NO_ROOTS;
        }

        @Override
        public void addChangeListener(ChangeListener l) {
            changes.addChangeListener(l);
        }

        @Override
        public void removeChangeListener(ChangeListener l) {
            changes.removeChangeListener(l);
        }
    }
}
