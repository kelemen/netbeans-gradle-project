package org.netbeans.gradle.project.util;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.Assert;
import org.junit.Test;


public class ExcludeIncludeTest {
    private static Path getAbsPath(String firstPart, String... lastParts) {
        return Paths.get(firstPart, lastParts).toAbsolutePath();
    }

    private static Path subPath(Path rootDir, String... subPaths) {
        Path result = rootDir;
        for (String subPath: subPaths) {
            result = result.resolve(subPath);
        }
        return result;
    }

    private static void assertInclude(
            Set<String> excludes,
            Set<String> includes,
            String... subPaths) {

        Path rootDir = getAbsPath("root", "subDir");
        Assert.assertTrue(ExcludeInclude.includeFile(
                subPath(rootDir, subPaths),
                rootDir,
                excludes,
                includes));
    }

    private static void assertExclude(
            Set<String> excludes,
            Set<String> includes,
            String... subPaths) {

        Path baseDir = getAbsPath("root");
        Path rootDir = subPath(baseDir, "include");
        Assert.assertFalse(ExcludeInclude.includeFile(
                subPath(baseDir, subPaths),
                rootDir,
                excludes,
                includes));
    }

    @Test
    public void testNoRules() {
        Set<String> excludes = Collections.emptySet();
        Set<String> includes = Collections.emptySet();

        assertInclude(excludes, includes, "path1", "path2");
        assertInclude(excludes, includes);

        assertExclude(excludes, includes, "others");
        assertExclude(excludes, includes, "others", "subDir");
    }

    private static Set<String> asSet(String... values) {
        return new HashSet<>(Arrays.asList(values));
    }

    @Test
    public void testFilesWithExcludedParent() {
        Set<String> excludes = asSet("**/test/*");
        Set<String> includes = Collections.emptySet();

        assertInclude(excludes, includes, "test", "subdir", "file1");
        assertInclude(excludes, includes, "root1", "test", "subdir", "file1");

        assertExclude(excludes, includes, "include", "test", "file1");
        assertExclude(excludes, includes, "include", "root1", "test", "file1");
        assertExclude(excludes, includes, "include", "root1", "root2", "test", "file1");
    }

    @Test
    public void testFilesWithSpecificParent() {
        Set<String> excludes = Collections.emptySet();
        Set<String> includes = asSet("**/test/*");

        assertInclude(excludes, includes, "test", "file1");
        assertInclude(excludes, includes, "root1", "test", "file1");
        assertInclude(excludes, includes, "root1", "root2", "test", "file1");

        assertExclude(excludes, includes, "include", "test", "subdir", "file1");
        assertExclude(excludes, includes, "include", "root1", "test", "subdir", "file1");
        assertExclude(excludes, includes, "include");
        assertExclude(excludes, includes, "include", "root1", "subDir");
    }

    @Test
    public void testFilesWithExcludedParentInPath() {
        Set<String> excludes = asSet("**/test/**");
        Set<String> includes = Collections.emptySet();

        assertInclude(excludes, includes);
        assertInclude(excludes, includes, "root1");
        assertInclude(excludes, includes, "root1", "root2");

        assertExclude(excludes, includes, "include", "test", "file1");
        assertExclude(excludes, includes, "include", "test", "subdir", "file1");
        assertExclude(excludes, includes, "include", "root1", "test", "subdir", "file1");
        assertExclude(excludes, includes, "include", "root1", "test", "file1");
        assertExclude(excludes, includes, "include", "root1", "root2", "test", "file1");
    }

    @Test
    public void testFilesWithSpecificParentInPath() {
        Set<String> excludes = Collections.emptySet();
        Set<String> includes = asSet("**/test/**");

        assertInclude(excludes, includes, "test", "file1");
        assertInclude(excludes, includes, "root1", "test", "file1");
        assertInclude(excludes, includes, "root1", "root2", "test", "file1");
        assertInclude(excludes, includes, "test", "subdir", "file1");
        assertInclude(excludes, includes, "root1", "test", "subdir", "file1");

        assertExclude(excludes, includes, "include");
        assertExclude(excludes, includes, "include", "root1", "subDir");
    }

    @Test
    public void testExcludeFromRoot() {
        Set<String> excludes = asSet("root1/*");
        Set<String> includes = Collections.emptySet();

        assertInclude(excludes, includes, "root2");
        assertInclude(excludes, includes, "root2", "file1");
        assertInclude(excludes, includes, "root2", "subdir", "file1");
        assertInclude(excludes, includes, "root1", "subdir", "file1");

        assertExclude(excludes, includes, "include", "root1", "file1");
    }

    @Test
    public void testIncludeFromRoot() {
        Set<String> excludes = Collections.emptySet();
        Set<String> includes = asSet("root1/*");

        assertInclude(excludes, includes, "root1", "file1");

        assertExclude(excludes, includes, "include", "root2");
        assertExclude(excludes, includes, "include", "root2", "file1");
        assertExclude(excludes, includes, "include", "root2", "subdir", "file1");
        assertExclude(excludes, includes, "include", "root1", "subdir", "file1");
    }

    @Test
    public void testIncludeFromMultipleRoot() {
        Set<String> excludes = Collections.emptySet();
        Set<String> includes = asSet("root1/*", "root2/*");

        assertInclude(excludes, includes, "root1", "file1");
        assertInclude(excludes, includes, "root2", "file1");

        assertExclude(excludes, includes, "include", "root3");
        assertExclude(excludes, includes, "include", "root3", "file1");
        assertExclude(excludes, includes, "include", "root3", "subdir", "file1");
        assertExclude(excludes, includes, "include", "root1", "subdir", "file1");
    }

    @Test
    public void testExcludeFromMultipleRoot() {
        Set<String> excludes = asSet("root1/*", "root2/*");
        Set<String> includes = Collections.emptySet();

        assertInclude(excludes, includes, "root3");
        assertInclude(excludes, includes, "root3", "file1");
        assertInclude(excludes, includes, "root3", "subdir", "file1");
        assertInclude(excludes, includes, "root1", "subdir", "file1");
        assertInclude(excludes, includes, "root2", "subdir", "file1");

        assertExclude(excludes, includes, "include", "root1", "file1");
        assertExclude(excludes, includes, "include", "root2", "file1");
    }

    @Test
    public void testIncludeExclude1() {
        Set<String> excludes = asSet("**/test/*");
        Set<String> includes = asSet("root1/**");

        assertInclude(excludes, includes, "root1", "file1");
        assertInclude(excludes, includes, "root1", "subdir", "file1");
        assertInclude(excludes, includes, "root1", "test", "subdir", "file1");

        assertExclude(excludes, includes, "include", "root2");
        assertExclude(excludes, includes, "include", "root2", "file1");
        assertExclude(excludes, includes, "include", "root2", "subdir", "file1");
        assertExclude(excludes, includes, "include", "root1", "test", "file1");
    }
}
