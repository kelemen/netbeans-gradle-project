package org.netbeans.gradle.project.util;

import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class MavenFileUtils {

    public static final String POM_DIR_NAME = "pom";
    public static final String SOURCES_CLASSIFIER = "-sources";
    public static final String JAVADOC_CLASSIFIER = "-javadoc";
    public static final String SNAPSHOT_SUFFIX = "-SNAPSHOT";

    public static final String SOURCES_SUFFIX = SOURCES_CLASSIFIER + ".jar";

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

    /**
     * Extracts the base name for the given binary if the binary is stored in a
     * custom local maven repository.
     *
     * @param binaryPath
     * @return The base name of the binary without the classifier if in a custom
     * local maven repository, else the name of the binary.
     */
    public static String binaryToBaseName(FileObject binaryPath) {
        // If the binary is in a local maven repo, then Gradle does not cache
        // and the binaries, sources and javadocs are stored in a format that 
        // looks like this:

        // ...BINARY_NAME\\BINARY_VERSION\\binary-javadoc.XXX
        // ...BINARY_NAME\\BINARY_VERSION\\binary-sources.XXX
        // ...BINARY_NAME\\BINARY_VERSION\\binary.XXX
        // ...BINARY_NAME\\BINARY_VERSION\\binary-win.XXX (optional)
        //
        // where binary is the string BINARY_NAME-BINARY_VERSION
        // There can be multiple binaries, for example the win binary here could 
        // be associated with the sources
        //
        // If the version is a snapshot (ends in -SNAPSHOT) and it's in a local 
        // maven repository, then binary is not BINARY_NAME-BINARY_VERSION. 
        // Instead it's:
        //
        // BINARY_NAME-BINARY_VERSION_WITHOUT_SNAPSHOT-DATE-SNAPSHOT_NUM
        //
        // for example:
        // foo/1.2-SNAPSHOT/foo-1.2-20110506.110000-3.jar
        // foo/1.2-SNAPSHOT/foo-1.2-20110506.110000-3-sources.jar
        // foo/1.2-SNAPSHOT/foo-1.2-20110506.110000-3-win.jar
        // foo/1.2-SNAPSHOT/foo-1.2-20110506.110000-3-javadoc.jar
        //
        // where the snapshot was the 3rd snapshot for this version, 
        // and it was generated at 2011/05/06 at 11:00:00)
        FileObject hashDir = binaryPath.getParent();
        if (hashDir == null) {
            return binaryPath.getName();
        }
        FileObject binDir = hashDir.getParent();
        if (binDir == null) {
            return binaryPath.getName();
        }

        //File stored in local maven repository, and version is snapshot
        if (hashDir.getNameExt().endsWith(SNAPSHOT_SUFFIX)) {
            String mavenVersion = hashDir.getNameExt().substring(0, hashDir.getNameExt().length() - SNAPSHOT_SUFFIX.length());
            String mavenLocalName = binDir.getNameExt() + "-" + mavenVersion;
            if (binaryPath.getNameExt().startsWith(mavenLocalName)) {
                Pattern p = Pattern.compile(Pattern.quote(mavenLocalName) + "-(\\d{8}\\.\\d{6}-[\\d]+)");
                Matcher m = p.matcher(binaryPath.getName());
                if (m.find()) {
                    String dateTimeSnapshot = m.group(1);
                    return mavenLocalName + "-" + dateTimeSnapshot;
                }
            }
        }

        //File stored in local maven repository
        String mavenLocalName = binDir.getNameExt() + "-" + hashDir.getNameExt();
        if (binaryPath.getNameExt().startsWith(mavenLocalName)) {
            return mavenLocalName;
        }
        return binaryPath.getName(); //not in a custom local maven repository
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

    public static Path toPath(FileObject fileObj) {
        return NbFileUtils.asPath(FileUtil.toFile(fileObj));
    }

    private MavenFileUtils() {
        throw new AssertionError();
    }
}
