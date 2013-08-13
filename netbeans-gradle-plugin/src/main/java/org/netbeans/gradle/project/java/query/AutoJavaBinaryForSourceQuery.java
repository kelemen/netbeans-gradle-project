package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.net.URL;
import java.util.Locale;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.BinaryForSourceQuery.Result;
import org.netbeans.gradle.project.query.AbstractBinaryForSourceQuery;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = BinaryForSourceQueryImplementation.class)})
public final class AutoJavaBinaryForSourceQuery extends AbstractBinaryForSourceQuery {
    private static final URL[] NO_ROOTS = new URL[0];

    public static final String SOURCES_SUFFIX = "-sources.zip";
    public static final String JAR_SUFFIX = ".jar";

    private static FileObject getJarForSource(FileObject sourceRoot) {
        String srcFileName = sourceRoot.getNameExt();
        if (!srcFileName.toLowerCase(Locale.US).endsWith(SOURCES_SUFFIX)) {
            return null;
        }

        String jarFileName = srcFileName.substring(0, srcFileName.length() - SOURCES_SUFFIX.length())
                + JAR_SUFFIX;

        FileObject dir = sourceRoot.getParent();
        if (dir == null) {
            return null;
        }

        return dir.getFileObject(jarFileName);
    }

    @Override
    protected Result tryFindBinaryRoots(File sourceRoot) {
        final FileObject sourceRootObj = FileUtil.toFileObject(sourceRoot);
        if (sourceRootObj == null) {
            return null;
        }

        if (getJarForSource(sourceRootObj) == null) {
            return null;
        }

        return new Result() {
            @Override
            public URL[] getRoots() {
                FileObject jar = getJarForSource(sourceRootObj);
                if (jar == null) {
                    return NO_ROOTS;
                }

                return new URL[]{jar.toURL()};
            }

            @Override
            public void addChangeListener(ChangeListener l) {
            }

            @Override
            public void removeChangeListener(ChangeListener l) {
            }
        };
    }
}
