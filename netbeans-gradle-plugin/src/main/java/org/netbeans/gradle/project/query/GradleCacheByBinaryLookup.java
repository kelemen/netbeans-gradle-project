package org.netbeans.gradle.project.query;

import java.io.File;
import java.util.Objects;
import java.util.function.Function;
import java.util.function.Supplier;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.LazyChangeSupport;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleCacheByBinaryLookup {

    private static final FileObject[] NO_ROOTS = new FileObject[0];
    private static final LazyChangeSupport CHANGES = LazyChangeSupport.createSwing(new EventSource());

    private final String searchedPackaging;
    private final Supplier<File> gradleUserHomeProvider;
    private final Function<FileObject, String> binaryToSearchedEntry;

    public GradleCacheByBinaryLookup(
            String searchedPackaging,
            Supplier<File> gradleUserHomeProvider,
            Function<FileObject, String> binaryToSearchedEntry) {
        this.searchedPackaging = Objects.requireNonNull(searchedPackaging, "searchedPackaging");
        this.gradleUserHomeProvider = Objects.requireNonNull(gradleUserHomeProvider, "gradleUserHomeProvider");
        this.binaryToSearchedEntry = Objects.requireNonNull(binaryToSearchedEntry, "binaryToSearchedEntry");
    }

    public static void notifyCacheChange() {
        CHANGES.fireChange();
    }

    public SourceForBinaryQueryImplementation2.Result tryFindEntryByBinary(File binaryRoot) {
        File gradleUserHome = gradleUserHomeProvider.get();
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

        final FileObject artifactRoot = binDir.getParent();
        if (artifactRoot == null) {
            return null;
        }

        String sourceFileName = binaryToSearchedEntry.apply(binaryRootObj);
        if (GradleFileUtils.isKnownBinaryDirName(binDir.getNameExt())) {
            return new OldFormatCacheResult(artifactRoot, searchedPackaging, sourceFileName);
        }
        else {
            return new NewFormatCacheResult(binDir, sourceFileName);
        }
    }

    private static final class EventSource
            implements
            SourceForBinaryQueryImplementation2.Result,
            LazyChangeSupport.Source {

        private volatile LazyChangeSupport changes;

        @Override
        public void init(LazyChangeSupport changes) {
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
