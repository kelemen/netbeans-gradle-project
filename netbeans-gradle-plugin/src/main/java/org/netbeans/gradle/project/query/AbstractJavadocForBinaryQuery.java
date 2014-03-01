package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation;
import org.openide.filesystems.FileUtil;

public abstract class AbstractJavadocForBinaryQuery implements JavadocForBinaryQueryImplementation {
    // This cache cannot shrink because JavadocForBinaryQueryImplementation
    // requires that we return the exact same object when the same URL is
    // querried. This is a very limiting constraint but I don't want to risk to
    // violate the constraint.
    private final ConcurrentMap<File, JavadocForBinaryQuery.Result> cache;

    public AbstractJavadocForBinaryQuery() {
        this.cache = new ConcurrentHashMap<>();
    }

    protected abstract JavadocForBinaryQuery.Result tryFindJavadoc(File binaryRoot);

    @Override
    public JavadocForBinaryQuery.Result findJavadoc(URL binaryRoot) {
        File binaryRootFile = FileUtil.archiveOrDirForURL(binaryRoot);
        if (binaryRootFile == null) {
            return null;
        }

        JavadocForBinaryQuery.Result result = cache.get(binaryRootFile);
        if (result != null) {
            return result;
        }

        result = tryFindJavadoc(binaryRootFile);
        if (result == null) {
            return null;
        }

        JavadocForBinaryQuery.Result oldResult = cache.putIfAbsent(binaryRootFile, result);
        return oldResult != null ? oldResult : result;
    }
}
