package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileUtil;

public abstract class AbstractBinaryForSourceQuery implements BinaryForSourceQueryImplementation {
    // This cache cannot shrink because SourceForBinaryQueryImplementation
    // requires that we return the exact same object when the same URL is
    // querried. This is a very limiting constraint but I don't want to risk to
    // violate the constraint.
    private final ConcurrentMap<File, BinaryForSourceQuery.Result> cache;

    public AbstractBinaryForSourceQuery() {
        this.cache = new ConcurrentHashMap<>();
    }

    protected abstract BinaryForSourceQuery.Result tryFindBinaryRoots(File sourceRoot);

    @Override
    public final BinaryForSourceQuery.Result findBinaryRoots(URL sourceRoot) {
        File sourceRootFile = FileUtil.archiveOrDirForURL(sourceRoot);
        if (sourceRootFile == null) {
            return null;
        }

        BinaryForSourceQuery.Result result = cache.get(sourceRootFile);
        if (result != null) {
            return result;
        }

        result = tryFindBinaryRoots(sourceRootFile);
        if (result == null) {
            return null;
        }

        BinaryForSourceQuery.Result oldResult = cache.putIfAbsent(sourceRootFile, result);
        return oldResult != null ? oldResult : result;
    }
}
