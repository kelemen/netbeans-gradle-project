package org.netbeans.gradle.project.output;

import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.util.Utilities;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

public final class ProjectFileConsumer implements SmartOutputHandler.Consumer {
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

    @Override
    public boolean tryConsumeLine(String line, OutputWriter output) throws IOException {
        String normalizedLine = line.replace(File.separatorChar, '/').toLowerCase(Locale.ROOT);
        int startIndex = normalizedLine.indexOf(normalizedPath);
        if (startIndex < 0) {
            return false;
        }

        int endPathIndex = normalizedLine.lastIndexOf('/');
        if (endPathIndex < 0) {
            // I don't think that this is possible but just in case it happens.
            return false;
        }

        int lineLength = line.length();
        int endIndex = lineLength;
        for (int i = endPathIndex + 1; i < lineLength; i++) {
            if (line.charAt(i) <= ' ') {
                endIndex = i;
                break;
            }
        }

        String fileStr = StringUtils.stripSeperatorsFromEnd(line.substring(startIndex, endIndex));
        File file = new File(fileStr);
        if (!file.isFile()) {
            return false;
        }

        OutputListener outputListener = null;
        if (isBrowserFile(fileStr)) {
            try {
                URL url = Utilities.toURI(file).toURL();
                outputListener = OutputUrlConsumer.getUrlListener(url);
            } catch (MalformedURLException ex) {
            }
        }

        if (outputListener == null) {
            outputListener = OpenEditorOutputListener.tryCreateListener(file, -1);
            if (outputListener == null) {
                return false;
            }
        }

        output.println(line, outputListener, false);
        return true;
    }
}
