package org.netbeans.gradle.project.output;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.Utilities;

public final class ProjectFileConsumer implements OutputLinkFinder {
    private final String normalizedPath;

    public ProjectFileConsumer(NbGradleProject project) {
        FileObject projectDirectory = project.getProjectDirectory();
        // In case the filesystem is not case-sesitive, otherwise it shouldn't
        // hurt much, since we will check if the file exists anyway.
        normalizedPath = projectDirectory.getPath().toLowerCase(Locale.ROOT);
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

    @Override
    public OutputLinkDef tryFindLink(String line) {
        String normalizedLine = line.replace(File.separatorChar, '/').toLowerCase(Locale.ROOT);
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
