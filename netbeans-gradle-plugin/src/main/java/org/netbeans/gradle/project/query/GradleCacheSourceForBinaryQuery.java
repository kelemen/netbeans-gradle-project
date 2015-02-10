package org.netbeans.gradle.project.query;

import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbFunction;
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
public final class GradleCacheSourceForBinaryQuery extends AbstractSourceForBinaryQuery {
    private static final FileObject[] NO_ROOTS = new FileObject[0];
    private static final ChangeSupport CHANGES;

    static {
        EventSource eventSource = new EventSource();
        CHANGES = new ChangeSupport(eventSource);
        eventSource.init(CHANGES);
    }

    private final NbFunction<FileObject, String> binaryToSourceName;

    public GradleCacheSourceForBinaryQuery() {
        this(new NbFunction<FileObject, String>() {
            @Override
            public String call(FileObject arg) {
                return GradleFileUtils.binaryToSourceName(arg);
            }
        });
    }

    public GradleCacheSourceForBinaryQuery(NbFunction<FileObject, String> binaryToSourceName) {
        ExceptionHelper.checkNotNullArgument(binaryToSourceName, "binaryToSourceName");

        this.binaryToSourceName = binaryToSourceName;
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
    protected Result tryFindSourceRoot(File binaryRoot) {
        File gradleUserHome = GradleFileUtils.GRADLE_USER_HOME.getValue();
        if (gradleUserHome == null) {
            return null;
        }

        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRoot);
        if (binaryRootObj == null) {
            return null;
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

        String sourceFileName = binaryToSourceName.call(binaryRootObj);

        if (GradleFileUtils.isKnownBinaryDirName(binDir.getNameExt())) {
            final FileObject artifactRoot = binDir.getParent();
            if (artifactRoot == null) {
                return null;
            }

            return new OldFormatCacheResult(artifactRoot, sourceFileName);
        }

        return new NewFormatCacheResult(binDir, sourceFileName);
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

    private static class NewFormatCacheResult implements Result {
        private final FileObject artifactRoot;
        private final String sourceFileName;

        public NewFormatCacheResult(FileObject artifactRoot, String sourceFileName) {
            this.artifactRoot = artifactRoot;
            this.sourceFileName = sourceFileName;
        }

        @Override
        public boolean preferSources() {
            return false;
        }

        @Override
        public FileObject[] getRoots() {
            // The cache directory of Gradle looks like this:
            //
            // ...... \\HASH_OF_SOURCE\\binary-sources.XXX
            // ...... \\HASH_OF_BINARY\\binary.XXX

            FileObject srcFile = NbFileUtils.getFileFromASubDir(artifactRoot, sourceFileName);
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
    }

    private static class OldFormatCacheResult implements Result {
        private final FileObject artifactRoot;
        private final String sourceFileName;

        public OldFormatCacheResult(FileObject artifactRoot, String sourceFileName) {
            this.artifactRoot = artifactRoot;
            this.sourceFileName = sourceFileName;
        }

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

            FileObject srcFile = NbFileUtils.getFileFromASubDir(srcDir, sourceFileName);
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
    }
}
