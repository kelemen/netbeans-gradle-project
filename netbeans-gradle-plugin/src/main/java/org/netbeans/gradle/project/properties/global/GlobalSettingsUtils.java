package org.netbeans.gradle.project.properties.global;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.utils.LazyValues;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.Places;

public final class GlobalSettingsUtils {
    private static final Logger LOGGER = Logger.getLogger(GlobalSettingsUtils.class.getName());

    private static final Supplier<Path> CONFIG_ROOT_REF = LazyValues.lazyValue(() -> {
        File rootFile = tryGetConfigRoot();
        return rootFile != null ? rootFile.toPath() : null;
    });

    private static final Supplier<Path> CACHE_ROOT_REF = LazyValues.lazyValue(GlobalSettingsUtils::tryGetCacheRoot);

    public static Path tryGetGlobalConfigPath(List<String> subPaths) {
        Path result = tryGetGlobalConfigPath0();
        if (result == null) {
            return null;
        }

        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    public static Path tryGetGlobalConfigPath(String... subPaths) {
        Path result = tryGetGlobalConfigPath0();
        if (result == null) {
            return null;
        }

        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    public static Path tryGetGlobalCachePath(List<String> subPaths) {
        Path result = CACHE_ROOT_REF.get();
        if (result == null) {
            return null;
        }

        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    public static Path tryGetGlobalCachePath(String... subPaths) {
        Path result = CACHE_ROOT_REF.get();
        if (result == null) {
            return null;
        }

        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    private static Path tryGetGlobalConfigPath0() {
        Path rootPath = CONFIG_ROOT_REF.get();
        if (rootPath == null) {
            LOGGER.log(Level.WARNING, "Unable to get config root folder.");
            return null;
        }

        return rootPath.resolve("org").resolve("netbeans").resolve("gradle");
    }

    private static File tryGetConfigRoot() {
        FileObject result = tryGetConfigRootObj();
        return result != null ? FileUtil.toFile(result) : null;
    }

    private static Path tryGetCacheRoot() {
        File globalRoot = Places.getCacheDirectory();
        if (globalRoot == null) {
            return null;
        }

        return globalRoot.toPath().resolve("gradle");
    }

    private static FileObject tryGetConfigRootObj() {
        try {
            return FileUtil.createFolder(FileUtil.getConfigRoot(), "Preferences");
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Cannot create global configuration folder: " + FileUtil.getConfigRoot(), ex);
            return null;
        }
    }

    private GlobalSettingsUtils() {
        throw new AssertionError();
    }
}
