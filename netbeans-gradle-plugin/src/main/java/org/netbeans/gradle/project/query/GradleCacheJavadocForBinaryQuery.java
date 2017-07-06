package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import java.util.function.Supplier;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({@ServiceProvider(service = JavadocForBinaryQueryImplementation.class, position = 70)})
public final class GradleCacheJavadocForBinaryQuery extends AbstractJavadocForBinaryQuery {
    private final GradleCacheByBinaryLookup sourceForBinary;
    private final GradleCacheByBinaryLookup javadocForBinary;

    public GradleCacheJavadocForBinaryQuery() {
        this(GradleFileUtils.GRADLE_USER_HOME_PROVIDER);
    }

    public GradleCacheJavadocForBinaryQuery(Supplier<File> gradleUserHomeProvider) {
        this.sourceForBinary = new GradleCacheByBinaryLookup(
                GradleFileUtils.SOURCE_DIR_NAME,
                gradleUserHomeProvider,
                GradleFileUtils::binaryToSourceName);
        this.javadocForBinary = new GradleCacheByBinaryLookup(
                GradleFileUtils.JAVADOC_DIR_NAME,
                gradleUserHomeProvider,
                GradleFileUtils::binaryToJavadocName);
    }

    private boolean hasSources(File binaryRootFile) {
        SourceForBinaryQueryImplementation2.Result result = sourceForBinary.tryFindEntryByBinary(binaryRootFile);
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

        final SourceForBinaryQueryImplementation2.Result result = javadocForBinary.tryFindEntryByBinary(binaryRoot);
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
