package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.BinaryForSourceQuery.Result;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = BinaryForSourceQueryImplementation.class)})
public final class GradleCacheBinaryForSourceQuery extends AbstractBinaryForSourceQuery {
    private static final URL[] NO_ROOTS = new URL[0];
    private static final ChangeSupport CHANGES;

    static {
        EventSource eventSource = new EventSource();
        CHANGES = new ChangeSupport(eventSource);
        eventSource.init(CHANGES);

        GradleFileUtils.GRADLE_USER_HOME.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                notifyCacheChange();
            }
        });
    }

    public GradleCacheBinaryForSourceQuery() {
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

        if (!GradleFileUtils.SOURCE_DIR_NAME.equals(srcDir.getNameExt())) {
            return null;
        }

        final FileObject artifactRoot = srcDir.getParent();
        if (artifactRoot == null) {
            return null;
        }

        final String binFileName = GradleFileUtils.sourceToBinaryName(sourceRootObj);
        if (binFileName == null) {
            return null;
        }

        return new Result() {
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

                    FileObject binFile = GradleFileUtils.getFileFromASubDir(binDir, binFileName);
                    if (binFile != null) {
                        return new URL[]{binFile.toURL()};
                    }
                    else {
                        continue;
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
        };
    }

    private static final class EventSource implements Result {
        private volatile ChangeSupport changes;

        public void init(ChangeSupport changes) {
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
}
