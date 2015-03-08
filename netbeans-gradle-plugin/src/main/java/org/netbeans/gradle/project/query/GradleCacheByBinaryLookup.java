package org.netbeans.gradle.project.query;

import java.io.File;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class GradleCacheByBinaryLookup {
    private static final FileObject[] NO_ROOTS = new FileObject[0];
    private static final ChangeSupport CHANGES;

    static {
        EventSource eventSource = new EventSource();
        CHANGES = new ChangeSupport(eventSource);
        eventSource.init(CHANGES);
    }

    private final String searchedPackaging;
    private final NbFunction<FileObject, String> binaryToSearchedEntry;

    public GradleCacheByBinaryLookup(String searchedPackaging, NbFunction<FileObject, String> binaryToSearchedEntry) {
        ExceptionHelper.checkNotNullArgument(searchedPackaging, "searchedPackaging");
        ExceptionHelper.checkNotNullArgument(binaryToSearchedEntry, "binaryToSearchedEntry");

        this.searchedPackaging = searchedPackaging;
        this.binaryToSearchedEntry = binaryToSearchedEntry;
    }

    public static void notifyCacheChange() {
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                CHANGES.fireChange();
            }
        });
    }

    public SourceForBinaryQueryImplementation2.Result tryFindEntryByBinary(File binaryRoot) {
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

        String sourceFileName = binaryToSearchedEntry.call(binaryRootObj);

        if (GradleFileUtils.isKnownBinaryDirName(binDir.getNameExt())) {
            final FileObject artifactRoot = binDir.getParent();
            if (artifactRoot == null) {
                return null;
            }

            return new OldFormatCacheResult(artifactRoot, searchedPackaging, sourceFileName);
        }

        return new NewFormatCacheResult(binDir, sourceFileName);
    }

    private static final class EventSource implements SourceForBinaryQueryImplementation2.Result {
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

    private static class NewFormatCacheResult implements SourceForBinaryQueryImplementation2.Result {
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

    private static class OldFormatCacheResult implements SourceForBinaryQueryImplementation2.Result {
        private final FileObject artifactRoot;
        private final String searchedPackaging;
        private final String searchedFileName;

        public OldFormatCacheResult(FileObject artifactRoot, String searchedPackaging, String searchedFileName) {
            this.artifactRoot = artifactRoot;
            this.searchedPackaging = searchedPackaging;
            this.searchedFileName = searchedFileName;
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
            FileObject searchedDir = artifactRoot.getFileObject(searchedPackaging);
            if (searchedDir == null) {
                return NO_ROOTS;
            }

            FileObject searchedFile = NbFileUtils.getFileFromASubDir(searchedDir, searchedFileName);
            return searchedFile != null ? new FileObject[]{searchedFile} : NO_ROOTS;
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
