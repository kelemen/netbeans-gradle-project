package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.gradle.project.util.SafeTmpFolder;
import org.netbeans.gradle.project.util.TestBinaryUtils;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.util.Utilities;

import static org.junit.Assert.*;
import static org.netbeans.gradle.project.query.TestSourceQueryUtils.*;

public class AutoJavaBinaryForSourceQueryTest {
    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

    @Test
    public void testFindBinaryOfZipSource() throws IOException {
        BinaryForSourceQueryImplementation query = new AutoJavaBinaryForSourceQuery();

        File rootDir = TMP_DIR_ROOT.newFolder();

        File jarPath = new File(rootDir, "myapp.jar");
        TestBinaryUtils.createTestJar(jarPath);

        File sourcesPath = new File(rootDir, "myapp-sources.zip");
        TestBinaryUtils.createTestZip(sourcesPath);

        URL sourceUrl = Utilities.toURI(sourcesPath).toURL();

        BinaryForSourceQuery.Result result = query.findBinaryRoots(sourceUrl);
        assertNotNull("result1", result);
        assertEquals("sourcesPath", jarPath, expectedSingleFile(result));
    }

    @Test
    public void testNoBinary() throws IOException {
        BinaryForSourceQueryImplementation query = new AutoJavaBinaryForSourceQuery();

        File rootDir = TMP_DIR_ROOT.newFolder();

        File sourcesPath = new File(rootDir, "myapp-sources.zip");
        TestBinaryUtils.createTestZip(sourcesPath);

        URL sourceUrl = Utilities.toURI(sourcesPath).toURL();
        assertNull("result1", query.findBinaryRoots(sourceUrl));
    }
}
