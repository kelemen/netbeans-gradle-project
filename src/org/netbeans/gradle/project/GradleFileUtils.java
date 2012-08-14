package org.netbeans.gradle.project;

import java.io.File;
import java.io.FileFilter;
import java.util.logging.Logger;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleFileUtils {
    private static final Logger LOGGER = Logger.getLogger(GradleFileUtils.class.getName());

    public static final File GRADLE_CACHE_HOME = getGradleCacheDir();
    public static final String BINARY_DIR_NAME = "jar";
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

    private GradleFileUtils() {
        throw new AssertionError();
    }
}
