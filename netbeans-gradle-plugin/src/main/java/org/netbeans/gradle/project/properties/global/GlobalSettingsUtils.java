package org.netbeans.gradle.project.properties.global;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.util.LazyValue;
import org.netbeans.gradle.project.util.NbSupplier;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GlobalSettingsUtils {
    private static final Logger LOGGER = Logger.getLogger(GlobalSettingsUtils.class.getName());

    private static final LazyValue<Path> CONFIG_ROOT_REF = new LazyValue<>(new NbSupplier<Path>() {
        @Override
        public Path get() {
            File rootFile = tryGetConfigRoot();
            return rootFile != null ? rootFile.toPath() : null;
        }
    });

    public static Path tryGetGlobalConfigPath(String... subPaths) {
        Path rootPath = CONFIG_ROOT_REF.get();
        if (rootPath == null) {
            LOGGER.log(Level.WARNING, "Unable to get config root folder.");
            return null;
        }

        Path result = rootPath.resolve("org").resolve("netbeans").resolve("gradle");
        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    private static File tryGetConfigRoot() {
        FileObject result = tryGetConfigRootObj();
        return result != null ? FileUtil.toFile(result) : null;
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
