package org.netbeans.gradle.project;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.BinaryForSourceQuery.Result;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = BinaryForSourceQueryImplementation.class)})
public final class GradleCacheBinaryForSourceQuery implements BinaryForSourceQueryImplementation {
    private static final URL[] NO_ROOTS = new URL[0];
    private static final ChangeSupport CHANGES = new ChangeSupport(ChangeSupport.class);

    // This cache cannot shrink because SourceForBinaryQueryImplementation
    // requires that we return the exact same object when the same URL is
    // querried. This is a very limiting constraint but I don't want to risk to
    // violate the constraint.
    private final ConcurrentMap<FileObject, Result> cache;

    public GradleCacheBinaryForSourceQuery() {
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
    public Result findBinaryRoots(URL sourceRoot) {
        if (GradleFileUtils.GRADLE_CACHE_HOME == null) {
            return null;
        }

        File sourceRootFile = FileUtil.archiveOrDirForURL(sourceRoot);
        if (sourceRootFile == null) {
            return null;
        }
        final FileObject sourceRootObj = FileUtil.toFileObject(sourceRootFile);
        if (sourceRootObj == null) {
            return null;
        }

        Result result = cache.get(sourceRootObj);
        if (result != null) {
            return result;
        }

        FileObject cacheHome = FileUtil.toFileObject(GradleFileUtils.GRADLE_CACHE_HOME);
        if (cacheHome == null || !FileUtil.isParentOf(cacheHome, sourceRootObj)) {
            return null;
        }

        result = new Result() {
            @Override
            public URL[] getRoots() {
                // The cache directory of Gradle looks like this:
                //
                // ...... \\source\\HASH_OF_SOURCE\\binary-sources.jar
                // ...... \\jar\\HASH_OF_BINARY\\binary.jar
                FileObject hashDir = sourceRootObj.getParent();
                if (hashDir == null) {
                    return NO_ROOTS;
                }

                FileObject srcDir = hashDir.getParent();
                if (srcDir == null) {
                    return NO_ROOTS;
                }

                if (!GradleFileUtils.SOURCE_DIR_NAME.equals(srcDir.getNameExt())) {
                    return NO_ROOTS;
                }

                FileObject artifactRoot = srcDir.getParent();
                if (artifactRoot == null) {
                    return NO_ROOTS;
                }

                FileObject binDir = artifactRoot.getFileObject(GradleFileUtils.BINARY_DIR_NAME);
                if (binDir == null) {
                    return NO_ROOTS;
                }

                String binFileName = GradleFileUtils.sourceToBinaryName(sourceRootObj);
                if (binFileName == null) {
                    return NO_ROOTS;
                }

                FileObject binFile = GradleFileUtils.getFileFromASubDir(binDir, binFileName);
                if (binFile != null) {
                    return new URL[]{binFile.toURL()};
                }
                else {
                    return NO_ROOTS;
                }
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

        Result oldResult = cache.putIfAbsent(sourceRootObj, result);
        return oldResult != null ? oldResult : result;
    }
}
