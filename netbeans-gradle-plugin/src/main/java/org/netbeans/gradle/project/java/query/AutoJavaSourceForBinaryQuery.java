package org.netbeans.gradle.project.java.query;

import java.io.File;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.query.AbstractSourceForBinaryQuery;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

import static org.netbeans.gradle.project.java.query.AutoJavaBinaryForSourceQuery.*;

@ServiceProviders({
    @ServiceProvider(service = SourceForBinaryQueryImplementation2.class),
    @ServiceProvider(service = SourceForBinaryQueryImplementation.class)})
public final class AutoJavaSourceForBinaryQuery extends AbstractSourceForBinaryQuery {
    private static final FileObject[] NO_ROOTS = new FileObject[0];

    public static FileObject sourceForJar(FileObject binaryRoot) {
        String srcFileName = binaryRoot.getName() + SOURCES_SUFFIX;

        FileObject dir = binaryRoot.getParent();
        if (dir == null) {
            return null;
        }

        FileObject result = dir.getFileObject(srcFileName);
        return result != null
                ? FileUtil.getArchiveRoot(result)
                : null;
    }

    @Override
    protected Result tryFindSourceRoot(File binaryRoot) {
        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRoot);
        if (binaryRootObj == null) {
            return null;
        }

        if (sourceForJar(binaryRootObj) == null) {
            return null;
        }

        return new Result() {
            @Override
            public boolean preferSources() {
                return false;
            }

            @Override
            public FileObject[] getRoots() {
                FileObject result = sourceForJar(binaryRootObj);
                return result != null
                        ? new FileObject[]{result}
                        : NO_ROOTS;
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
