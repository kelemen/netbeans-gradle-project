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

    // TODO: Instead of protected methods, they should be provided as an argument.

    protected File normalizeSourcePath(File sourceRoot) {
        return sourceRoot;
    }

    protected abstract BinaryForSourceQuery.Result tryFindBinaryRoots(File sourceRoot);

    @Override
    public final BinaryForSourceQuery.Result findBinaryRoots(URL sourceRoot) {
        File sourceRootFile = FileUtil.archiveOrDirForURL(sourceRoot);
        if (sourceRootFile == null) {
            return null;
        }

        File normSourceRoot = normalizeSourcePath(sourceRootFile);
        if (normSourceRoot == null) {
            return null;
        }

        BinaryForSourceQuery.Result result = cache.get(normSourceRoot);
        if (result != null) {
            return result;
        }

        result = tryFindBinaryRoots(normSourceRoot);
        if (result == null) {
            return null;
        }

        BinaryForSourceQuery.Result oldResult = cache.putIfAbsent(normSourceRoot, result);
        return oldResult != null ? oldResult : result;
    }
}
