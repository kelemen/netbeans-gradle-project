package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.HashSet;
import java.util.Set;
import java.util.function.Supplier;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.api.java.queries.SourceForBinaryQuery;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.util.JavaModelTestUtils;
import org.netbeans.gradle.project.util.SafeTmpFolder;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Utilities;

import static org.junit.Assert.*;

public class GradleSourceForBinaryQueryTest {
    @ClassRule
    public static final SafeTmpFolder TMP_DIR_ROOT = new SafeTmpFolder();

    public GradleSourceForBinaryQueryTest() {
    }

    private Supplier<NbJavaModule> testModule(File rootDir) throws IOException {
        NbJavaModule module = JavaModelTestUtils.createModule(rootDir);
        return () -> module;
    }

    private void verifyAllClassesDirHaveSources(SourceForBinaryQueryImplementation2 query, NbJavaModule module) throws IOException {
        for (JavaSourceSet sourceSet: module.getSources()) {
            try {
                verifyAllClassesDirHaveSources(query, sourceSet);
            } catch (Throwable ex) {
                throw new AssertionError("Test failed for source set: " + sourceSet.getName(), ex);
            }
        }
    }

    private static Set<File> toFiles(FileObject[] objs) {
        Set<File> result = new HashSet<>();
        for (FileObject obj: objs) {
            File file = FileUtil.toFile(obj);
            if (file == null) {
                throw new AssertionError(obj.getPath() + " does not exist");
            }
            result.add(file);
        }
        return result;
    }

    private void verifyAllClassesDirHaveSources(SourceForBinaryQueryImplementation2 query, JavaSourceSet sourceSet) throws IOException {
        Set<File> expectedSourceRoots = JavaModelTestUtils.getAllSourceDirs(sourceSet);

        for (File classesDir: sourceSet.getOutputDirs().getClassesDirs()) {
            verifySourceRoots(query, classesDir, expectedSourceRoots);
            verifySourceRoots(query, new File(classesDir, "subdir"), expectedSourceRoots);
        }
    }

    private void verifySourceRoots(
            SourceForBinaryQueryImplementation2 query,
            File binaryDir,
            Set<File> expectedSourceRoots) throws IOException {

        URL binaryDirUrl = Utilities.toURI(binaryDir).toURL();

        SourceForBinaryQuery.Result queryResult = query.findSourceRoots(binaryDirUrl);
        assertNotNull("result for classes dir", queryResult);
        assertEquals("srcDirs", expectedSourceRoots, toFiles(queryResult.getRoots()));

        SourceForBinaryQueryImplementation2.Result queryResult2 = query.findSourceRoots2(binaryDirUrl);
        assertNotNull("result2 for classes dir", queryResult2);
        assertEquals("srcDirs", expectedSourceRoots, toFiles(queryResult2.getRoots()));
        assertTrue("prefer sources", queryResult2.preferSources());
    }

    @Test
    public void testSourcesForClassesDirs() throws IOException {
        File rootDir = TMP_DIR_ROOT.newFolder();
        Supplier<NbJavaModule> moduleRef = testModule(rootDir);
        NbJavaModule module = moduleRef.get();

        GradleSourceForBinaryQuery query = new GradleSourceForBinaryQuery(moduleRef);
        verifyAllClassesDirHaveSources(query, module);
    }

    private void verifyDoesNotHaveSourceRoot(
            SourceForBinaryQueryImplementation2 query,
            File dir) throws IOException {

        URL dirUrl = Utilities.toURI(dir).toURL();

        SourceForBinaryQuery.Result queryResult = query.findSourceRoots(dirUrl);
        assertNull("result for classes dir", queryResult);

        SourceForBinaryQueryImplementation2.Result queryResult2 = query.findSourceRoots2(dirUrl);
        assertNull("result2 for classes dir", queryResult2);
    }

    @Test
    public void verifyDoesNotHaveSourceRoot() throws IOException {
        File rootDir = TMP_DIR_ROOT.newFolder();
        Supplier<NbJavaModule> moduleRef = testModule(rootDir);
        NbJavaModule module = moduleRef.get();

        GradleSourceForBinaryQuery query = new GradleSourceForBinaryQuery(moduleRef);
        verifyDoesNotHaveSourceRoot(query, module.getModuleDir());
    }
}
