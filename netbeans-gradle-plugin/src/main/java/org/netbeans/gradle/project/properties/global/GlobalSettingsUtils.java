package org.netbeans.gradle.project.properties.global;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.util.LazyPaths;
import org.netbeans.gradle.project.util.NbSupplier;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.modules.Places;

public final class GlobalSettingsUtils {
    private static final Logger LOGGER = Logger.getLogger(GlobalSettingsUtils.class.getName());

    private static final LazyPaths CONFIG_ROOT = new LazyPaths(new NbSupplier<Path>() {
        @Override
        public Path get() {
            File rootFile = tryGetConfigRoot();
            if (rootFile == null) {
                return null;
            }

            return rootFile.toPath().resolve("org").resolve("netbeans").resolve("gradle");
        }
    });

    private static final LazyPaths CACHE_DIR = new LazyPaths(new NbSupplier<Path>() {
        @Override
        public Path get() {
            return tryGetCacheRoot();
        }
    });

    public static LazyPaths configRoot() {
        return CONFIG_ROOT;
    }

    public static LazyPaths cacheRoot() {
        return CACHE_DIR;
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
