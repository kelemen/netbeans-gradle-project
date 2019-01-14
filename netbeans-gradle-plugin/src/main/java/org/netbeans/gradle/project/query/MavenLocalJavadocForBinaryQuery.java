package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.gradle.project.util.MavenFileUtils;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = JavadocForBinaryQueryImplementation.class)})
public final class MavenLocalJavadocForBinaryQuery extends AbstractJavadocForBinaryQuery {

    final static MavenLocalSourceForBinaryQuery SOURCE_QUERY;

    static {
        SOURCE_QUERY = new MavenLocalSourceForBinaryQuery();
    }

    public MavenLocalJavadocForBinaryQuery() {
    }

    private boolean hasSources(File binaryRootFile) {
        SourceForBinaryQueryImplementation2.Result result = SOURCE_QUERY.tryFindSourceRoot(binaryRootFile);
        if (result == null) {
            return false;
        }
        return result.getRoots().length > 0;
    }

    @Override
    protected JavadocForBinaryQuery.Result tryFindJavadoc(File binaryRoot) {
        if (hasSources(binaryRoot)) {
            // TODO: Global settings should be added to allow prefer javadoc
            //       over sources.
            return null;
        }

        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRoot);
        if (binaryRootObj == null) {
            return null;
        }

        String javadocName = MavenFileUtils.binaryToJavadocName(binaryRootObj);
        if (javadocName == null) {
            return null;
        }

        File binaryRootParent = binaryRoot.getParentFile();
        File javadocFile = new File(binaryRootParent, javadocName);
        FileObject javadocFileObj = FileUtil.toFileObject(javadocFile);
        if (javadocFileObj == null) {
            return null;
        }

        FileObject asArchive = NbFileUtils.asArchiveOrDir(javadocFileObj);
        if (asArchive == null) {
            return null;
        }

        return new JavadocForBinaryQuery.Result() {
            @Override
            public URL[] getRoots() {
                return new URL[]{asArchive.toURL()};
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
