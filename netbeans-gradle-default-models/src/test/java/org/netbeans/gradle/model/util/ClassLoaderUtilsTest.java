package org.netbeans.gradle.model.util;

import java.io.File;
import java.net.URL;
import java.util.Arrays;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.junit.Assume.*;

@SuppressWarnings("deprecation")
public class ClassLoaderUtilsTest {
    private static void verifyPathParts(File file, String... parts) {
        File current = file;
        for (int i = parts.length - 1; i >= 0; i--) {
            assertNotNull("File is too short " + Arrays.toString(parts), current);
            assertEquals("File must match file parts: " + file, parts[i], current.getName());
            current = current.getParentFile();
        }

        assertTrue("File path is too long " + file, current == null || current.getParentFile() == null);
    }

    @Test
    public void testExtractJarPathFromUrl1() throws Exception {
        File file = ClassLoaderUtils.extractPathFromURL(new URL("jar:file:///c/my!dir/my-path!/"));
        verifyPathParts(file, "c", "my!dir", "my-path");
    }

    @Test
    public void testExtractJarPathFromUrl2() throws Exception {
        File file = ClassLoaderUtils.extractPathFromURL(new URL("jar:file:///c/mydir!/my-path!/"));
        verifyPathParts(file, "c", "mydir!", "my-path");
    }

    @Test
    public void testExtractPathFromURLWithSpaces() throws Exception {
        File file = ClassLoaderUtils.extractPathFromURL(new URL("file:///c/my%20dir/my-path"));
        verifyPathParts(file, "c", "my dir", "my-path");
    }

    @Test
    public void testExtractPathFromURLWithSpecialChars() throws Exception {
        File file = ClassLoaderUtils.extractPathFromURL(new URL("file:///c/my-path/\u00e1b\u0678\u040c\u039e\u05de"));
        verifyPathParts(file, "c", "my-path", "\u00e1b\u0678\u040c\u039e\u05de");
    }

    @Test
    public void testGetLocationOfClassPath() {
        File dir = ClassLoaderUtils.getLocationOfClassPath();
        assertTrue("Class path must exist", dir.exists());
    }

    @Test
    public void testGetLocationOfClassPathInDirectory() {
        File dir = ClassLoaderUtils.getLocationOfClassPath();
        assumeTrue("The project binaries must be in a directory and not a jar for this test to be executed",
                dir.isDirectory());

        String relPath = ClassLoaderUtils.class.getName().replace(".", File.separator) + ".class";
        File classFile = new File(dir, relPath);

        assertTrue("Class file must be on the class path: " + dir, classFile.isFile());
    }

    @Test
    public void testFindClassPathOfClassForJar() {
        File jarOfJUnit = ClassLoaderUtils.findClassPathOfClass(org.junit.Assert.class);
        assertTrue("Jar must exist: " + jarOfJUnit, jarOfJUnit.isFile());
        assertTrue("Must have extension jar: " + jarOfJUnit, jarOfJUnit.getName().endsWith(".jar"));
    }
}
