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
        File jarDir = BasicFileUtils.getSubPath(artifactRoot, "57436");
        File jar = BasicFileUtils.getSubPath(jarDir, "myproj.jar");

        jarDir.mkdirs();
        TestBinaryUtils.createTestJar(jar);

        File srcDir = BasicFileUtils.getSubPath(artifactRoot, "25754");
        File srcFile = BasicFileUtils.getSubPath(srcDir, "myproj-sources.jar");

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
}
