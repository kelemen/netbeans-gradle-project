package org.netbeans.gradle.project.util;

import java.io.File;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleFileUtils {
    private static final Logger LOGGER = Logger.getLogger(GradleFileUtils.class.getName());

    private static final File NORM_USER_HOME = getUserHome();

    public static final PropertySource<File> GRADLE_USER_HOME = getGradleUserHome();
    public static final Supplier<File> GRADLE_USER_HOME_PROVIDER = GRADLE_USER_HOME::getValue;

    public static final Set<String> BINARY_DIR_NAMES = Collections.unmodifiableSet(
            new HashSet<>(Arrays.asList("jar", "bundle")));
    public static final String POM_DIR_NAME = "pom";
    public static final String JAVADOC_DIR_NAME = "javadoc";
    public static final String SOURCE_DIR_NAME = "source";
    public static final String SOURCES_CLASSIFIER = "-sources";
    public static final String JAVADOC_CLASSIFIER = "-javadoc";

    public static final String SOURCES_SUFFIX = SOURCES_CLASSIFIER + ".jar";

    private static File getUserHome() {
        String userHome = System.getProperty("user.home");
        if (userHome == null) {
            LOGGER.severe("user.home property is missing.");
            return null;
        }

        File notNormalizedResult = new File(userHome);
        File result = FileUtil.normalizeFile(notNormalizedResult);
        if (result == null) {
            LOGGER.log(Level.SEVERE, "Could not normalize path: {0}", notNormalizedResult);
            return null;
        }
        return result;
    }

    private static File getDefaultGradleUserHome() {
        return new File(NORM_USER_HOME, ".gradle");
    }

    private static PropertyReference<File> gradleUserHomeDir() {
        return CommonGlobalSettings.getDefault().gradleUserHomeDir();
    }

    private static PropertySource<File> getGradleUserHome() {
        return new PropertySource<File>() {
            @Override
            public File getValue() {
                File value = gradleUserHomeDir().getActiveValue();
                if (value == null) {
                    value = getDefaultGradleUserHome();
                }
                return value;
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return gradleUserHomeDir().getActiveSource().addChangeListener(listener);
            }
        };
    }

    public static FileObject getGradleUserHomeFileObject() {
        File result = GRADLE_USER_HOME.getValue();
        return result != null
                ? FileUtil.toFileObject(result)
                : null;
    }

    public static boolean isKnownBinaryDirName(String dirName) {
        Objects.requireNonNull(dirName, "dirName");

        String lowerDirName = dirName.toLowerCase(Locale.US);
        return BINARY_DIR_NAMES.contains(lowerDirName);
    }

    public static boolean isSourceFile(FileObject sourcePath) {
        String name = sourcePath.getNameExt();
        return name.endsWith(SOURCES_SUFFIX);
    }

    public static String binaryToSourceName(FileObject binaryPath) {
        String binFileExt = binaryPath.getExt();
        return binaryToBaseName(binaryPath) + SOURCES_CLASSIFIER + "." + binFileExt;
    }

    public static String binaryToJavadocName(FileObject binaryPath) {
        String binFileExt = binaryPath.getExt();
        return binaryToBaseName(binaryPath) + JAVADOC_CLASSIFIER + "." + binFileExt;
    }

    public static String binaryToBaseName(FileObject binaryPath) {
        FileObject hashDir = binaryPath.getParent();
        if (hashDir == null) {
            return binaryPath.getName();
        }

        FileObject binDir = hashDir.getParent();
        if (binDir == null) {
            return binaryPath.getName();
        }

        if (GradleFileUtils.isKnownBinaryDirName(binDir.getNameExt())) {
            return binaryPath.getName();
        }
        else {
            final FileObject versionDir = binDir;
            final FileObject artifactRoot = binDir.getParent();
            if (artifactRoot == null) {
                return binaryPath.getName();
            }
            return artifactRoot.getNameExt() + "-" + versionDir.getNameExt();
        }
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

    public static Path toPath(FileObject fileObj) {
        return NbFileUtils.asPath(FileUtil.toFile(fileObj));
    }

    private GradleFileUtils() {
        throw new AssertionError();
    }
}
