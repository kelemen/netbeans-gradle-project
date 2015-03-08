package org.netbeans.gradle.project.util;

import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.util.Collection;

public final class ExcludeInclude {
    public static boolean includeFile(
            Path file,
            Path rootPath,
            Collection<String> excludePatterns,
            Collection<String> includePatterns) {

        Path absoluteRoot = rootPath.toAbsolutePath();
        Path testedPath = file.toAbsolutePath();

        if (!testedPath.startsWith(absoluteRoot)) {
            return false;
        }

        Path relTestedPath = absoluteRoot.relativize(testedPath);

        if (!includePatterns.isEmpty()) {
            if (!matchesAnyAntPattern(relTestedPath, includePatterns)) {
                return false;
            }
        }

        return !matchesAnyAntPattern(relTestedPath, excludePatterns);
    }

    private static boolean matchesAnyAntPattern(
            Path path,
            Collection<String> patterns) {

        for (String pattern: patterns) {
            if (matchesAntPattern(path, pattern)) {
                return true;
            }
        }
        return false;
    }

    private static boolean matchesAntPattern(Path path, String pattern) {
        FileSystem fileSystem = path.getFileSystem();
        PathMatcher matcher = fileSystem.getPathMatcher(toMatchStr(pattern));
        return matcher.matches(path);
    }

    private static String toMatchStr(String pattern) {
        String normPattern = pattern.replace("\\\\", "/");

        // 7 = "glob:".length() + "**".length()
        StringBuilder result = new StringBuilder(pattern.length() + 7);
        result.append("glob:");

        String normedDirMatches = normPattern;
        if (normedDirMatches.startsWith("/")) {
            normedDirMatches = normedDirMatches.substring(1);
        }

        normedDirMatches = normedDirMatches.replace("/**/", "{/**/,/}");
        if (normedDirMatches.startsWith("**/")) {
            normedDirMatches = "{**/,}" + normedDirMatches.substring(3);
        }

        result.append(normedDirMatches);
        if (normPattern.endsWith("/")) {
            result.append("**");
        }
        return result.toString();
    }

    private ExcludeInclude() {
        throw new AssertionError();
    }
}
