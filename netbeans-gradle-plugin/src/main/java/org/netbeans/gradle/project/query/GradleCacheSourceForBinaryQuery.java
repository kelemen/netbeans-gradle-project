package org.netbeans.gradle.project.query;

import java.io.File;
import java.util.function.Supplier;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = SourceForBinaryQueryImplementation2.class, position = 70),
    @ServiceProvider(service = SourceForBinaryQueryImplementation.class, position = 70)})
public final class GradleCacheSourceForBinaryQuery extends AbstractSourceForBinaryQuery {
    private final GradleCacheByBinaryLookup sourceLookup;

    public GradleCacheSourceForBinaryQuery() {
        this(GradleFileUtils.GRADLE_USER_HOME_PROVIDER);
    }

    public GradleCacheSourceForBinaryQuery(Supplier<File> gradleUserHomeProvider) {
        this.sourceLookup = new GradleCacheByBinaryLookup(
                GradleFileUtils.SOURCE_DIR_NAME,
                gradleUserHomeProvider,
                GradleFileUtils::binaryToSourceName);
    }

    @Override
    protected Result tryFindSourceRoot(File binaryRoot) {
        return sourceLookup.tryFindEntryByBinary(binaryRoot);
    }
}
