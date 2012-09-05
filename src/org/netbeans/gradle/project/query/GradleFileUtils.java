package org.netbeans.gradle.project.query;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleFileUtils {
    private static final Logger LOGGER = Logger.getLogger(GradleFileUtils.class.getName());

    public static final File GRADLE_CACHE_HOME = getGradleCacheDir();
    public static final Set<String> BINARY_DIR_NAMES =  Collections.unmodifiableSet(
            new HashSet<String>(Arrays.asList("jar", "bundle")));
    public static final String POM_DIR_NAME = "pom";
    public static final String SOURCE_DIR_NAME = "source";
    public static final String SOURCES_CLASSIFIER = "-sources";

    private static File getGradleCacheDir() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            LOGGER.severe("Gradle home path could not have been constructed.");
            return null;
        }
        return new File(new File(userHome), ".gradle");
    }

    public static boolean canBeBinaryDirName(String dirName) {
        if (dirName == null) throw new NullPointerException("dirName");

        boolean result = !POM_DIR_NAME.equals(dirName)
                && !SOURCE_DIR_NAME.equals(dirName);
        if (result && LOGGER.isLoggable(Level.WARNING)) {
            if (!BINARY_DIR_NAMES.contains(dirName)) {
                LOGGER.log(Level.WARNING, "{0} is assumed to be a possible binary container folder of the cache.", dirName);
            }
        }
        return result;
    }

    public static String binaryToSourceName(FileObject binaryPath) {
        String binFileName = binaryPath.getName();
        String binFileExt = binaryPath.getExt();
        return binFileName + SOURCES_CLASSIFIER + "." + binFileExt;
    }

    public static String sourceToBinaryName(FileObject sourcePath) {
        String srcFileName = sourcePath.getName();
        if (!srcFileName.endsWith(SOURCES_CLASSIFIER)) {
            return null;
        }
        String srcFileExt = sourcePath.getExt();

        return srcFileName.substring(0, srcFileName.length() - SOURCES_CLASSIFIER.length())
                + "." + srcFileExt;
    }

    public static FileObject asArchiveOrDir(FileObject file) {
        if (file == null) {
            return null;
        }

        if (FileUtil.isArchiveFile(file)) {
            return FileUtil.getArchiveRoot(file);
        }
        else {
            return file;
        }
    }

    public static FileObject getFileFromASubDir(FileObject root, String fileName) {
        File rootDir = FileUtil.toFile(root);
        if (rootDir == null) {
            return null;
        }

        File[] subDirs = rootDir.listFiles(new FileFilter() {
            @Override
            public boolean accept(File pathname) {
                return pathname.isDirectory();
            }
        });

        for (File dir: subDirs) {
            File srcFileName = new File(dir, fileName);
            if (srcFileName.isFile()) {
                return asArchiveOrDir(FileUtil.toFileObject(srcFileName));
            }
        }
        return null;
    }

    // We assume that the gradle home directory looks like this:
    //
    // binaries: GRADLE_HOME\\lib\\*.jar
    // sources for all binaries: GRADLE_HOME\\src

    public static File getLibDirOfGradle(File gradleHome) {
        return new File(gradleHome, "lib");
    }

    public static FileObject getLibDirOfGradle(FileObject gradleHome) {
        return gradleHome.getFileObject("lib");
    }

    public static File getSrcDirOfGradle(File gradleHome) {
        return new File(gradleHome, "src");
    }

    public static FileObject getSrcDirOfGradle(FileObject gradleHome) {
        return gradleHome.getFileObject("src");
    }

    public static void createDefaultSourceDirs(FileObject projectDir) throws IOException {
        FileObject srcDir = projectDir.createFolder("src");
        FileObject mainDir = srcDir.createFolder("main");
        FileObject testDir = srcDir.createFolder("test");

        mainDir.createFolder("java");
        mainDir.createFolder("resources");

        testDir.createFolder("java");
        testDir.createFolder("resources");
    }

    private GradleFileUtils() {
        throw new AssertionError();
    }
}
