package org.netbeans.gradle.project.util;

import java.nio.file.Path;
import java.util.List;

public final class LazyPaths {
    private final NbSupplier<Path> rootProvider;

    public LazyPaths(NbSupplier<? extends Path> rootProvider) {
        this.rootProvider = new LazyValue<>(rootProvider);
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
