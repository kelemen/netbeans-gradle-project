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
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

import static org.junit.Assert.*;
import static org.netbeans.gradle.project.query.TestSourceQueryUtils.*;

public class GradleHomeSourceForBinaryQueryTest {
    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

    private GradleHomeSourceForBinaryQuery createWithRoot(File rootDir) {
        final FileObject rootObj = rootDir != null ? FileUtil.toFileObject(rootDir) : null;
        return new GradleHomeSourceForBinaryQuery(() -> rootObj);
    }

    private void doTestFindSourceOfStandardLibInSubDir(String... jarSubDir) throws IOException {
        File rootDir = TMP_DIR_ROOT.newFolder();
        File libDir = new File(rootDir, "lib");
        File srcDir = new File(rootDir, "src");

        File jarDir = BasicFileUtils.getSubPath(libDir, jarSubDir);

        jarDir.mkdirs();
        srcDir.mkdirs();

        SourceForBinaryQueryImplementation2 query = createWithRoot(rootDir);

        File jarPath = new File(jarDir, "testbin.jar");
        TestBinaryUtils.createTestJar(jarPath);

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(Utilities.toURI(jarPath).toURL());
        expectSameArchive("srcDir", srcDir, expectedSingleFile(result1));

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(Utilities.toURI(jarPath).toURL());
        expectSameArchive("srcDir", srcDir, expectedSingleFile(result2));
        assertFalse("preferSources", result2.preferSources());
    }

    @Test
    public void testFindSourceOfStandardLib() throws IOException {
        doTestFindSourceOfStandardLibInSubDir();
    }

    @Test
    public void testFindSourceOfStandardLibInSubDir() throws IOException {
        doTestFindSourceOfStandardLibInSubDir("subdir");
    }

    @Test
    public void testNoGradleHome() throws IOException {
        File rootDir = TMP_DIR_ROOT.newFolder();
        File libDir = new File(rootDir, "lib");
        File srcDir = new File(rootDir, "src");

        libDir.mkdirs();
        srcDir.mkdirs();

        SourceForBinaryQueryImplementation2 query = createWithRoot(null);

        File jarPath = new File(libDir, "testbin.jar");
        TestBinaryUtils.createTestJar(jarPath);

        URL binaryUrl = Utilities.toURI(jarPath).toURL();

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNull("result1", result1);

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNull("result2", result2);
    }

    @Test
    public void testNotInLibDir() throws IOException {
        File rootDir = TMP_DIR_ROOT.newFolder();
        SourceForBinaryQueryImplementation2 query = createWithRoot(rootDir);

        File jarPath = new File(rootDir, "myapp.jar");
        TestBinaryUtils.createTestJar(jarPath);

        URL binaryUrl = Utilities.toURI(jarPath).toURL();

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNull("result1", result1);

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNull("result2", result2);
    }

    @Test
    public void testMissingSrcDir() throws IOException {
        File rootDir = TMP_DIR_ROOT.newFolder();
        File libDir = new File(rootDir, "lib");

        libDir.mkdirs();

        SourceForBinaryQueryImplementation2 query = createWithRoot(rootDir);

        File jarPath = new File(libDir, "testbin.jar");
        TestBinaryUtils.createTestJar(jarPath);

        URL binaryUrl = Utilities.toURI(jarPath).toURL();

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNull("result1", result1);

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNull("result2", result2);
    }
}
