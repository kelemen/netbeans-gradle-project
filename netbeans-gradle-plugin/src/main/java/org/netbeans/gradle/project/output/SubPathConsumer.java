package org.netbeans.gradle.project.output;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.util.Utilities;

public final class SubPathConsumer implements OutputLinkFinder {
    private final String normalizedPath;

    private SubPathConsumer(String normalizedPath) {
        this.normalizedPath = Objects.requireNonNull(normalizedPath, "normalizedPath");
    }

    private static Set<String> filterRedundantDirs(Collection<Path> roots) {
        Set<String> result = new HashSet<>();
        for (Path root: roots) {
            addNonRedundant(result, root);
        }
        return result;
    }

    private static void addNonRedundant(Set<String> roots, Path newRoot) {
        String normNewRoot = normalizePath(newRoot.toString());
        if (!isRedundant(roots, normNewRoot)) {
            roots.add(normNewRoot);
        }
    }

    private static boolean isRedundant(Set<String> roots, String newRoot) {
        for (String currentRoot: roots) {
            if (currentRoot.startsWith(newRoot) || newRoot.startsWith(currentRoot)) {
                return true;
            }
        }
        return false;
    }

    public static OutputLinkFinder pathLinks(Collection<Path> roots) {
        Set<String> uniqueDirs = filterRedundantDirs(roots);
        final List<OutputLinkFinder> linkFinders = new ArrayList<>(uniqueDirs.size());
        for (final String root: uniqueDirs) {
            linkFinders.add(new SubPathConsumer(root));
        }

        if (linkFinders.size() == 1) {
            return linkFinders.get(0);
        }

        return new OutputLinkFinder() {
            @Override
            public OutputLinkDef tryFindLink(String line) {
                for (OutputLinkFinder linkFinder: linkFinders) {
                    OutputLinkDef result = linkFinder.tryFindLink(line);
                    if (result != null) {
                        return result;
                    }
                }
                return null;
            }
        };
    }

    public static boolean isBrowserFile(String path) {
        String lowerPath = path.toLowerCase(Locale.US);
        return lowerPath.endsWith(".html")
                || lowerPath.endsWith(".xhtml")
                || lowerPath.endsWith(".htm");
    }

    private static boolean isLineSeparator(char ch) {
        return ch <= ' ' || ch == ':' || ch == ';';
    }

    private static int tryFindEndLineIndex(String line, int startIndex) {
        int lineLength = line.length();
        int actualStartIndex = startIndex;
        for (; actualStartIndex < lineLength; actualStartIndex++) {
            if (line.charAt(actualStartIndex) > ' ') {
                break;
            }
        }

        if (actualStartIndex >= lineLength) {
            return -1;
        }

        for (int i = actualStartIndex; i < lineLength; i++) {
            char ch = line.charAt(i);
            if (ch < '0' || ch > '9') {
                return i;
            }
        }
        return lineLength;
    }

    private static ParsedIntDef tryReadNumber(String line, int startIndex) {
        int endOfNumberIndex = tryFindEndLineIndex(line, startIndex);
        if (endOfNumberIndex <= startIndex) {
            return null;
        }

        String intStr = line.substring(startIndex, endOfNumberIndex);

        try {
            return new ParsedIntDef(Integer.parseInt(intStr.trim()), intStr.length());
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static String normalizePath(String path) {
        // In case the filesystem is not case-sesitive, otherwise it shouldn't
        // hurt much, since we will check if the file exists anyway.
        return path.replace(File.separatorChar, '/').toLowerCase(Locale.ROOT);
    }

    @Override
    public OutputLinkDef tryFindLink(String line) {
        String normalizedLine = normalizePath(line);
        int startIndex = normalizedLine.indexOf(normalizedPath);
        if (startIndex < 0) {
            return null;
        }

        int endPathIndex = normalizedLine.lastIndexOf('/');
        if (endPathIndex < 0) {
            // I don't think that this is possible but just in case it happens.
            return null;
        }

        int lineLength = line.length();
        int endIndex = lineLength;
        for (int i = endPathIndex + 1; i < lineLength; i++) {
            if (isLineSeparator(line.charAt(i))) {
                endIndex = i;
                break;
            }
        }

        ParsedIntDef lineNumberDef = tryReadNumber(line, endIndex + 1);

        int completeLinkEndIndex = endIndex;
        int lineNumber = -1;
        if (lineNumberDef != null) {
            completeLinkEndIndex = endIndex + 1 + lineNumberDef.strLength;
            lineNumber = lineNumberDef.value;
        }
        if (lineNumber < 0) {
            lineNumber = -1;
        }

        String unstrippedFileStr = line.substring(startIndex, endIndex);
        String fileStr = StringUtils.stripSeperatorsFromEnd(unstrippedFileStr);
        completeLinkEndIndex = completeLinkEndIndex - (unstrippedFileStr.length() - fileStr.length());

        File file = new File(fileStr);
        if (!file.isFile()) {
            return null;
        }

        Runnable outputListener = null;
        if (isBrowserFile(fileStr)) {
            try {
                URL url = Utilities.toURI(file).toURL();
                outputListener = OutputUrlConsumer.getUrlOpenTask(url);
            } catch (MalformedURLException ex) {
            }
        }

        if (outputListener == null) {
            outputListener = OpenEditorOutputListener.tryCreateListener(file, lineNumber);
            if (outputListener == null) {
                return null;
            }
        }

        return new OutputLinkDef(startIndex, completeLinkEndIndex, outputListener);
    }

    private static final class ParsedIntDef {
        public final int value;
        public final int strLength;

        public ParsedIntDef(int value, int strLength) {
            this.value = value;
            this.strLength = strLength;
        }
    }
}
