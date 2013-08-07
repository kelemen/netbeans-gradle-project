package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.net.URL;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.gradle.project.query.AbstractJavadocForBinaryQuery;
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = JavadocForBinaryQueryImplementation.class)})
public final class AutoJavaJavadocForBinaryQuery extends AbstractJavadocForBinaryQuery {
    private static final URL[] NO_ROOTS = new URL[0];

    private static final String JAVADOC_SUFFIX = "-javadoc.zip";

    public static FileObject javadocForJar(FileObject binaryRoot) {
        String srcFileName = binaryRoot.getName() + JAVADOC_SUFFIX;

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
    protected JavadocForBinaryQuery.Result tryFindJavadoc(File binaryRoot) {
        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRoot);
        if (binaryRootObj == null) {
            return null;
        }

        // TODO: Adjust global settings to allow prefer javadoc over sources.
        if (AutoJavaSourceForBinaryQuery.sourceForJar(binaryRootObj) != null) {
            return null;
        }

        if (javadocForJar(binaryRootObj) == null) {
            return null;
        }

        return new JavadocForBinaryQuery.Result() {
            @Override
            public URL[] getRoots() {
                FileObject javadoc = javadocForJar(binaryRootObj);
                if (javadoc == null) {
                    return NO_ROOTS;
                }

                return new URL[]{javadoc.toURL()};
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
