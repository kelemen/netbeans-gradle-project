package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.BinaryForSourceQuery.Result;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.LazyChangeSupport;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = BinaryForSourceQueryImplementation.class)})
public final class GradleCacheBinaryForSourceQuery extends AbstractBinaryForSourceQuery {
    private static final URL[] NO_ROOTS = new URL[0];
    private static final LazyChangeSupport CHANGES;

    static {
        CHANGES = LazyChangeSupport.createSwing(new EventSource());
        GradleFileUtils.GRADLE_USER_HOME.addChangeListener(GradleCacheBinaryForSourceQuery::notifyCacheChange);
    }

    public GradleCacheBinaryForSourceQuery() {
    }

    public static void notifyCacheChange() {
        CHANGES.fireChange();
    }

    @Override
    protected Result tryFindBinaryRoots(File sourceRoot) {
        File gradleUserHome = GradleFileUtils.GRADLE_USER_HOME.getValue();
        if (gradleUserHome == null) {
            return null;
        }

        final FileObject sourceRootObj = FileUtil.toFileObject(sourceRoot);
        if (sourceRootObj == null) {
            return null;
        }

        FileObject gradleUserHomeObj = FileUtil.toFileObject(gradleUserHome);
        if (gradleUserHomeObj == null || !FileUtil.isParentOf(gradleUserHomeObj, sourceRootObj)) {
            return null;
        }

        FileObject hashDir = sourceRootObj.getParent();
        if (hashDir == null) {
            return null;
        }

        FileObject srcDir = hashDir.getParent();
        if (srcDir == null) {
            return null;
        }

        final String binFileName = GradleFileUtils.sourceToBinaryName(sourceRootObj);
        if (binFileName == null) {
            return null;
        }

        if (GradleFileUtils.SOURCE_DIR_NAME.equals(srcDir.getNameExt())) {
            final FileObject artifactRoot = srcDir.getParent();
            if (artifactRoot == null) {
                return null;
            }

            return new OldFormatCacheResult(artifactRoot, binFileName);
        }

        return GradleFileUtils.isSourceFile(sourceRootObj)
                ? new NewFormatCacheResult(srcDir, binFileName)
                : null;
    }

    private static final class EventSource
    implements
            Result,
            LazyChangeSupport.Source {

        private volatile LazyChangeSupport changes;

        @Override
        public void init(LazyChangeSupport changes) {
            assert changes != null;
            this.changes = changes;
        }

        @Override
        public URL[] getRoots() {
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
        private final String binFileName;

        public NewFormatCacheResult(FileObject artifactRoot, String binFileName) {
            this.artifactRoot = artifactRoot;
            this.binFileName = binFileName;
        }

        @Override
        public URL[] getRoots() {
            // The cache directory of Gradle looks like this:
            //
            // ...... \\HASH_OF_SOURCE\\binary-sources.XXX
            // ...... \\HASH_OF_BINARY\\binary.XXX

            FileObject binFile = NbFileUtils.getFileFromASubDir(artifactRoot, binFileName);
            return binFile != null
                    ? new URL[]{binFile.toURL()}
                    : NO_ROOTS;
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
        private final String binFileName;

        public OldFormatCacheResult(FileObject artifactRoot, String binFileName) {
            this.artifactRoot = artifactRoot;
            this.binFileName = binFileName;
        }

        @Override
        public URL[] getRoots() {
            // The cache directory of Gradle looks like this:
            //
            // ...... \\source\\HASH_OF_SOURCE\\binary-sources.jar
            // ...... \\packaging type\\HASH_OF_BINARY\\binary.jar

            for (String binDirName: GradleFileUtils.BINARY_DIR_NAMES) {
                FileObject binDir = artifactRoot.getFileObject(binDirName);
                if (binDir == null) {
                    continue;
                }

                FileObject binFile = NbFileUtils.getFileFromASubDir(binDir, binFileName);
                if (binFile != null) {
                    return new URL[]{binFile.toURL()};
                }
            }
            return NO_ROOTS;
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
