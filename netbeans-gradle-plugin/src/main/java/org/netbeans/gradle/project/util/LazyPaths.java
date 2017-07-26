package org.netbeans.gradle.project.util;

import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import org.jtrim2.utils.LazyValues;

public final class LazyPaths {
    private final Supplier<Path> rootProvider;

    public LazyPaths(Supplier<? extends Path> rootProvider) {
        this.rootProvider = LazyValues.lazyValue(rootProvider);
    }

    public Path tryGetRoot() {
        return rootProvider.get();
    }

    public Path tryGetSubPath(List<String> subPaths) {
        Path result = tryGetRoot();
        if (result == null) {
            return null;
        }

        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    public Path tryGetSubPath(String... subPaths) {
        Path result = tryGetRoot();
        if (result == null) {
            return null;
        }

        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    @Override
    public String toString() {
        return "LazyPaths{" + rootProvider + '}';
    }
}
