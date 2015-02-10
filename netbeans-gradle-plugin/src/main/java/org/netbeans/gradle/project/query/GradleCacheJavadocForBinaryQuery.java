package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = JavadocForBinaryQueryImplementation.class)})
public final class GradleCacheJavadocForBinaryQuery extends AbstractJavadocForBinaryQuery {
    private final GradleCacheSourceForBinaryQuery sourceForBinary;
    private final GradleCacheSourceForBinaryQuery javadocForBinary;

    public GradleCacheJavadocForBinaryQuery() {
        this.sourceForBinary = new GradleCacheSourceForBinaryQuery();
        this.javadocForBinary = new GradleCacheSourceForBinaryQuery(new NbFunction<FileObject, String>() {
            @Override
            public String call(FileObject arg) {
                return GradleFileUtils.binaryToJavadocName(arg);
            }
        });
    }

    private boolean hasSources(File binaryRootFile) {
        return sourceForBinary.tryFindSourceRoot(binaryRootFile) != null;
    }

    @Override
    protected JavadocForBinaryQuery.Result tryFindJavadoc(File binaryRoot) {
        if (hasSources(binaryRoot)) {
            // TODO: Global settings should be added to allow prefer javadoc
            //       over sources.
            return null;
        }

        final SourceForBinaryQueryImplementation2.Result result = javadocForBinary.tryFindSourceRoot(binaryRoot);
        if (result == null) {
            return null;
        }

        return new JavadocForBinaryQuery.Result() {
            @Override
            public URL[] getRoots() {
                FileObject[] roots = result.getRoots();
                if (roots == null) {
                    return null;
                }

                URL[] resultUrls = new URL[roots.length];
                for (int i = 0; i < roots.length; i++) {
                    resultUrls[i] = roots[i].toURL();
                }
                return resultUrls;
            }

            @Override
            public void addChangeListener(ChangeListener l) {
                result.addChangeListener(l);
            }

            @Override
            public void removeChangeListener(ChangeListener l) {
                result.removeChangeListener(l);
            }
        };
    }
}
