package org.netbeans.gradle.project.query;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.gradle.model.util.BasicFileUtils;
import org.netbeans.gradle.project.util.SafeTmpFolder;
import org.netbeans.gradle.project.util.TestBinaryUtils;
import org.openide.util.Utilities;

import static org.junit.Assert.*;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import static org.netbeans.gradle.project.query.TestSourceQueryUtils.*;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;

public class MavenLocalBinaryForSourceQueryTest {

    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

    private static BinaryForSourceQueryImplementation createBinaryQuery() {
        return new MavenLocalBinaryForSourceQuery();
    }

    private void verifyBinary(URL sourceUrl, File binaryFile) {
        BinaryForSourceQueryImplementation query = createBinaryQuery();

        BinaryForSourceQuery.Result result1 = query.findBinaryRoots(sourceUrl);
        assertNotNull("result1", result1);
        expectSameArchive("binaryPath", binaryFile, expectedSingleFile(result1));
    }

    private void verifyNotMavenLocal(URL sourceUrl) {
        BinaryForSourceQueryImplementation query = createBinaryQuery();

        BinaryForSourceQuery.Result result1 = query.findBinaryRoots(sourceUrl);
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

        File srcFile = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-sources.jar");

        TestBinaryUtils.createTestJar(srcFile);

        URL srcUrl = Utilities.toURI(srcFile).toURL();
        verifyBinary(srcUrl, jar);
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

        URL srcUrl = Utilities.toURI(srcFile).toURL();
        verifyBinary(srcUrl, jar);
    }

    @Test
    public void testNewCacheFormatRemoteSnapshot() throws IOException {
        //this should be dealt with by the gradle cache
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

        URL srcUrl = Utilities.toURI(srcFile).toURL();
        verifyNotMavenLocal(srcUrl);
    }

    @Test
    public void testNewCacheFormatMissingSourceInMavenLocal() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);
        
        File srcFile = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-sources.jar");
        //not created

        URL srcUrl = Utilities.toURI(srcFile).toURL();
        verifyNotMavenLocal(srcUrl);
    }

    @Test
    public void testNewCacheFormatMissingSourceInMavenLocalSnapshot() throws IOException {
        File gradleHome = TMP_DIR_ROOT.newFolder();

        File artifactRoot = BasicFileUtils.getSubPath(gradleHome, "org", "myproj");
        File versionDir = BasicFileUtils.getSubPath(artifactRoot, "11.2-SNAPSHOT");
        File jar = BasicFileUtils.getSubPath(versionDir, "myproj-11.2-20110506.110000-7.jar");

        versionDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);
        
        File srcDir = BasicFileUtils.getSubPath(versionDir, "25754");
        File srcFile = BasicFileUtils.getSubPath(srcDir, "foo-11.2-SNAPSHOT-sources.jar");

        URL srcUrl = Utilities.toURI(srcFile).toURL();
        verifyNotMavenLocal(srcUrl);
    }
}
