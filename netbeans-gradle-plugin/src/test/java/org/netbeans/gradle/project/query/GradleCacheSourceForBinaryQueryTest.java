package org.netbeans.gradle.project.query;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.gradle.model.util.BasicFileUtils;
import org.netbeans.gradle.project.util.SafeTmpFolder;
import org.netbeans.gradle.project.util.TestBinaryUtils;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.util.Utilities;

import static org.junit.Assert.*;
import org.junit.Ignore;
import static org.netbeans.gradle.project.query.TestSourceQueryUtils.*;

public class GradleCacheSourceForBinaryQueryTest {

    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

    private static SourceForBinaryQueryImplementation2 createWithRoot(final File gradleHomeRoot) {
        return new GradleCacheSourceForBinaryQuery(() -> gradleHomeRoot);
    }

    private void verifySource(File gradleHome, URL binaryUrl, File srcFile) {
        SourceForBinaryQueryImplementation2 query = createWithRoot(gradleHome);

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNotNull("result1", result1);
        expectSameArchive("sourcesPath", srcFile, expectedSingleFile(result1));

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNotNull("result2", result2);
        expectSameArchive("sourcesPath", srcFile, expectedSingleFile(result2));
        assertFalse("preferSources", result2.preferSources());
    }

    private void verifyNotDownloadedSource(File gradleHome, URL binaryUrl) {
        SourceForBinaryQueryImplementation2 query = createWithRoot(gradleHome);

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNotNull("result1", result1);
        assertEquals("sourcesPath", 0, result1.getRoots().length);

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNotNull("result2", result2);
        assertEquals("sourcesPath", 0, result2.getRoots().length);
        assertFalse("preferSources", result2.preferSources());
    }

    @Test
    public void testOldCacheFormat() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File jarDir = BasicFileUtils.getSubPath(artifactRoot, "jar", "43253");
        File jar = BasicFileUtils.getSubPath(jarDir, "myproj.jar");

        jarDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File srcDir = BasicFileUtils.getSubPath(artifactRoot, "source", "643632");
        File srcFile = BasicFileUtils.getSubPath(srcDir, "myproj-sources.jar");

        srcDir.mkdirs();
        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifySource(gradleHome, binaryUrl, srcFile);
    }

    @Test
    public void testOldCacheFormatMissingSource() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File jarDir = BasicFileUtils.getSubPath(artifactRoot, "jar", "43253");
        File jar = BasicFileUtils.getSubPath(jarDir, "myproj.jar");

        jarDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyNotDownloadedSource(gradleHome, binaryUrl);
    }

    @Test
    public void testNewCacheFormat() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2");
        File jarDir = BasicFileUtils.getSubPath(versionDir, "57436");
        File jar = BasicFileUtils.getSubPath(jarDir, "myproj-11.2.jar");

        jarDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File srcDir = BasicFileUtils.getSubPath(versionDir, "25754");
        File srcFile = BasicFileUtils.getSubPath(srcDir, "myproj-11.2-sources.jar");

        srcDir.mkdirs();
        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifySource(gradleHome, binaryUrl, srcFile);
    }

    @Test
    public void testNewCacheFormatMissingSource() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File jarDir = BasicFileUtils.getSubPath(artifactRoot, "57436");
        File jar = BasicFileUtils.getSubPath(jarDir, "myproj.jar");

        jarDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyNotDownloadedSource(gradleHome, binaryUrl);
    }

    @Test
    public void testNewCacheFormatMultipleBinaries() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org.openjfx", "javafx-base");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11");
        File jarDir1 = BasicFileUtils.getSubPath(versionDir, "57436");
        File binary1 = BasicFileUtils.getSubPath(jarDir1, "javafx-base-11-win.jar");

        File jarDir2 = BasicFileUtils.getSubPath(versionDir, "63457");
        File binary2 = BasicFileUtils.getSubPath(jarDir2, "javafx-base-11.jar");

        jarDir1.mkdirs();
        TestBinaryUtils.createTestJar(binary1);

        jarDir2.mkdirs();
        TestBinaryUtils.createTestJar(binary2);

        File srcDir = BasicFileUtils.getSubPath(versionDir, "25754");
        File srcFile = BasicFileUtils.getSubPath(srcDir, "javafx-base-11-sources.jar");

        srcDir.mkdirs();
        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl1 = Utilities.toURI(binary1).toURL();
        verifySource(gradleHome, binaryUrl1, srcFile);

        URL binaryUrl2 = Utilities.toURI(binary2).toURL();
        verifySource(gradleHome, binaryUrl2, srcFile);
    }

    @Test
    @Ignore
    public void testNotInCache() throws IOException {
        File root = TMP_DIR_ROOT.newFolder();

        File gradleHome = new File(root, ".gradle");
        gradleHome.mkdirs();

        File binaryHome = new File(root, "otherdir");

        File artifactRoot = BasicFileUtils.getSubPath(binaryHome, "org", "myproj");
        File jarDir = BasicFileUtils.getSubPath(artifactRoot, "57436");
        File jar = BasicFileUtils.getSubPath(jarDir, "myproj.jar");

        jarDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        URL binaryUrl = Utilities.toURI(jar).toURL();

        SourceForBinaryQueryImplementation2 query = createWithRoot(gradleHome);
        assertNull("result1", query.findSourceRoots(binaryUrl));
        assertNull("result2", query.findSourceRoots2(binaryUrl));
    }

    @Test
    public void testNewCacheFormatMavenLocal() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File srcFile = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-sources.jar");

        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifySource(gradleHome, binaryUrl, srcFile);
    }

    @Test
    public void testNewCacheFormatMavenLocalMultipleBinaries() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-win.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File srcFile = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-sources.jar");

        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifySource(gradleHome, binaryUrl, srcFile);
    }

    @Test
    public void testNewCacheFormatMavenLocalSnapshot() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "foo");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2-SNAPSHOT");
        File jar = BasicFileUtils.getSubPath(versionDir, "foo-11.2-20110506.110000-3.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File srcFile = BasicFileUtils.getSubPath(versionDir, "foo-11.2-20110506.110000-3-sources.jar");

        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifySource(gradleHome, binaryUrl, srcFile);
    }

    @Test
    public void testNewCacheFormatMavenLocalSnapshotMultipleBinaries() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "foo");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2-SNAPSHOT");
        File jar = BasicFileUtils.getSubPath(versionDir, "foo-11.2-20110506.110000-3-win.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File srcFile = BasicFileUtils.getSubPath(versionDir, "foo-11.2-20110506.110000-3-sources.jar");

        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifySource(gradleHome, binaryUrl, srcFile);
    }

    @Test
    public void testNewCacheFormatRemoteSnapshot() throws IOException {
        //SNAPSHOT versions shouldn't work any different from non snapshot versions in the gradle cache.
        //only the latest version is stored in the gradle cache, 
        //and SNAPSHOT is part of the binary name (unlike the maven local case).
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "foo");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2-SNAPSHOT");
        File jarDir = BasicFileUtils.getSubPath(versionDir, "57436");
        File jar = BasicFileUtils.getSubPath(jarDir, "foo-11.2-SNAPSHOT.jar");

        jarDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File srcDir = BasicFileUtils.getSubPath(versionDir, "25754");
        File srcFile = BasicFileUtils.getSubPath(srcDir, "foo-11.2-SNAPSHOT-sources.jar");

        srcDir.mkdirs();
        TestBinaryUtils.createTestJar(srcFile);

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifySource(gradleHome, binaryUrl, srcFile);
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
        verifyNotDownloadedSource(gradleHome, binaryUrl);
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
        verifyNotDownloadedSource(gradleHome, binaryUrl);
    }
}
