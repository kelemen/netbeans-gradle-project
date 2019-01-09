package org.netbeans.gradle.project.query;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.api.java.queries.JavadocForBinaryQuery;
import org.netbeans.gradle.model.util.BasicFileUtils;
import org.netbeans.gradle.project.util.SafeTmpFolder;
import org.netbeans.gradle.project.util.TestBinaryUtils;
import org.netbeans.spi.java.queries.JavadocForBinaryQueryImplementation;
import org.openide.util.Utilities;

import static org.junit.Assert.*;
import static org.netbeans.gradle.project.query.TestSourceQueryUtils.*;

public class MavenLocalJavadocForBinaryQueryTest {

    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

    private static JavadocForBinaryQueryImplementation createJavadocQuery() {
        return new MavenLocalJavadocForBinaryQuery();
    }

    private void verifyJavadoc(URL binaryUrl, File javadocFile, boolean hasSources) {
        JavadocForBinaryQueryImplementation query = createJavadocQuery();

        JavadocForBinaryQuery.Result result = query.findJavadoc(binaryUrl);
        if (hasSources) {
            //if it has sources, the javadoc shouldn't be used
            assertNull("result", result);
        } else {
            assertNotNull("result", result);
            expectSameArchive("javadocPath", javadocFile, expectedSingleFile(result));
        }
    }

    private void verifyNotDownloadedJavadoc(URL binaryUrl) {
        JavadocForBinaryQueryImplementation query = createJavadocQuery();
        JavadocForBinaryQuery.Result result1 = query.findJavadoc(binaryUrl);
        assertNull("result1", result1);
    }

    @Test
    public void testNewCacheFormatMavenLocal() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File javadocFile = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-javadoc.jar");

        TestBinaryUtils.createTestJar(javadocFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyJavadoc(binaryUrl, javadocFile, false);
    }

    @Test
    public void testNewCacheFormatMavenLocalWithSources() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File javadocFile = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-javadoc.jar");

        TestBinaryUtils.createTestJar(javadocFile);

        File srcFile = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-sources.jar");

        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyJavadoc(binaryUrl, javadocFile, true);
    }

    @Test
    public void testNewCacheFormatMavenLocalMultipleBinaries() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-win.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File javadocFile = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-javadoc.jar");

        TestBinaryUtils.createTestJar(javadocFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyJavadoc(binaryUrl, javadocFile, false);
    }

    @Test
    public void testNewCacheFormatMavenLocalSnapshot() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "foo");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2-SNAPSHOT");
        File jar = BasicFileUtils.getSubPath(versionDir, "foo-11.2-20110506.110000-3.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File javadocFile = BasicFileUtils.getSubPath(versionDir, "foo-11.2-20110506.110000-3-javadoc.jar");

        TestBinaryUtils.createTestJar(javadocFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyJavadoc(binaryUrl, javadocFile, false);
    }

    @Test
    public void testNewCacheFormatMavenLocalSnapshotMultipleBinaries() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "foo");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2-SNAPSHOT");
        File jar = BasicFileUtils.getSubPath(versionDir, "foo-11.2-20110506.110000-3-win.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File javadocFile = BasicFileUtils.getSubPath(versionDir, "foo-11.2-20110506.110000-3-javadoc.jar");

        TestBinaryUtils.createTestJar(javadocFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyJavadoc(binaryUrl, javadocFile, false);
    }

    @Test
    public void testNewCacheFormatMissingSourceInMavenLocal() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyNotDownloadedJavadoc(binaryUrl);
    }

    @Test
    public void testNewCacheFormatMissingSourceInMavenLocalSnapshot() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2-SNAPSHOT");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-20110506.110000-7.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyNotDownloadedJavadoc(binaryUrl);
    }
}
