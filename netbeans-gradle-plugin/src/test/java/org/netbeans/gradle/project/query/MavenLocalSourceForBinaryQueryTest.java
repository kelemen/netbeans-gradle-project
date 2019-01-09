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
import static org.netbeans.gradle.project.query.TestSourceQueryUtils.*;

public class MavenLocalSourceForBinaryQueryTest {

    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

    private static SourceForBinaryQueryImplementation2 createSourceQuery() {
        return new MavenLocalSourceForBinaryQuery();
    }

    private void verifySource(URL binaryUrl, File srcFile) {
        SourceForBinaryQueryImplementation2 query = createSourceQuery();

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNotNull("result1", result1);
        expectSameArchive("sourcesPath", srcFile, expectedSingleFile(result1));

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNotNull("result2", result2);
        expectSameArchive("sourcesPath", srcFile, expectedSingleFile(result2));
        assertFalse("preferSources", result2.preferSources());
    }

    private void verifyNotDownloadedSource(URL binaryUrl) {
        SourceForBinaryQueryImplementation2 query = createSourceQuery();

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNull("result1", result1);

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNull("result2", result2);
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
        verifySource(binaryUrl, srcFile);
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
        verifySource(binaryUrl, srcFile);
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
        verifySource(binaryUrl, srcFile);
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
        verifySource(binaryUrl, srcFile);
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

        URL binaryUrl = Utilities.toURI(jar).toURL();
        verifyNotDownloadedSource(binaryUrl);
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
        verifyNotDownloadedSource(binaryUrl);
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
        verifyNotDownloadedSource(binaryUrl);
    }
}
