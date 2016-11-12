package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URL;
import org.junit.ClassRule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.gradle.project.util.TestBinaryUtils;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

import static org.junit.Assert.*;

public class AutoJavaSourceForBinaryQueryTest {
    @ClassRule
    public static final TemporaryFolder TMP_DIR_ROOT = new TemporaryFolder();

    private static FileObject expectedSingleFile(SourceForBinaryQuery.Result result) {
        return expectedSingleFile(result.getRoots());
    }

    private static FileObject expectedSingleFile(FileObject[] fileObjs) {
        assertEquals("Expected exactly one file object", 1, fileObjs.length);
        return fileObjs[0];
    }

    private static void expectSameArchive(String name, File expected, FileObject actual) {
        URI expectedUri = FileUtil.getArchiveRoot(FileUtil.toFileObject(expected)).toURI();
        URI actualUri = actual.toURI();
        assertEquals(name, expectedUri, actualUri);
    }

    @Test
    public void testFindZipSource() throws IOException {
        SourceForBinaryQueryImplementation2 query = new AutoJavaSourceForBinaryQuery();

        File rootDir = TMP_DIR_ROOT.newFolder();

        File jarPath = new File(rootDir, "myapp.jar");
        TestBinaryUtils.createTestJar(jarPath);

        File sourcesPath = new File(rootDir, "myapp-sources.zip");
        TestBinaryUtils.createTestZip(sourcesPath);

        URL binaryUrl = Utilities.toURI(jarPath).toURL();

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNotNull("result1", result1);
        expectSameArchive("sourcesPath", sourcesPath, expectedSingleFile(result1));

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNotNull("result2", result2);
        expectSameArchive("sourcesPath", sourcesPath, expectedSingleFile(result2));
        assertFalse("preferSources", result2.preferSources());
    }

    @Test
    public void testNoZipSource() throws IOException {
        SourceForBinaryQueryImplementation2 query = new AutoJavaSourceForBinaryQuery();

        File rootDir = TMP_DIR_ROOT.newFolder();

        File jarPath = new File(rootDir, "myapp.jar");
        TestBinaryUtils.createTestJar(jarPath);

        URL binaryUrl = Utilities.toURI(jarPath).toURL();

        SourceForBinaryQuery.Result result1 = query.findSourceRoots(binaryUrl);
        assertNull("result1", result1);

        SourceForBinaryQueryImplementation2.Result result2 = query.findSourceRoots2(binaryUrl);
        assertNull("result2", result2);
    }
}
