package org.netbeans.gradle.project.query;

import java.io.File;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.NbFunction;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = SourceForBinaryQueryImplementation2.class),
    @ServiceProvider(service = SourceForBinaryQueryImplementation.class)})
public final class GradleCacheSourceForBinaryQuery extends AbstractSourceForBinaryQuery {
    private final GradleCacheByBinaryLookup sourceLookup;

    public GradleCacheSourceForBinaryQuery() {
        this.sourceLookup = new GradleCacheByBinaryLookup(GradleFileUtils.SOURCE_DIR_NAME, binaryToSourceName());
    }

    public static NbFunction<FileObject, String> binaryToSourceName() {
        return new NbFunction<FileObject, String>() {
            @Override
            public String call(FileObject arg) {
                return GradleFileUtils.binaryToSourceName(arg);
            }
        };
    }

    @Override
    protected Result tryFindSourceRoot(File binaryRoot) {
        return sourceLookup.tryFindEntryByBinary(binaryRoot);
    }
}
