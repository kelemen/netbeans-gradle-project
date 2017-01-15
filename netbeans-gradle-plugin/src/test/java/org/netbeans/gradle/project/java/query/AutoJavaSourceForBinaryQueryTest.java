package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.gradle.project.util.SafeTmpFolder;
import org.netbeans.gradle.project.util.TestBinaryUtils;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.util.Utilities;

import static org.junit.Assert.*;
import static org.netbeans.gradle.project.query.TestSourceQueryUtils.*;

public class AutoJavaSourceForBinaryQueryTest {
    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

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
