package org.netbeans.gradle.project.output;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FileLineConsumer implements OutputLinkFinder {
    private static final Logger LOGGER = Logger.getLogger(FileLineConsumer.class.getName());

    private static OutputLinkDef tryFindLink(String line, File file, String otherInfo) {
        int lineIndexSep = otherInfo.indexOf(':');
        int lineNumber = -1;
        if (lineIndexSep > 0) {
            try {
                lineNumber = Integer.parseInt(otherInfo.substring(0, lineIndexSep).trim());
            } catch (NumberFormatException ex) {
            }
        }

        Runnable listener = OpenEditorOutputListener.tryCreateListener(file, lineNumber);
        if (listener == null) {
            LOGGER.log(Level.WARNING, "File displayed in the output disappeared: {0}", file);
            return null;
        }

        // TODO: Altough we expect the whole line to point to a line of a file
        //       we should be more precise.
        return new OutputLinkDef(0, line.length(), listener);
    }

    private OutputLinkDef tryFindLink(String line, int sepIndex) {
        File file = new File(line.substring(0, sepIndex).trim());
        if (file.isFile()) {
            return tryFindLink(line, file, line.substring(sepIndex + 1, line.length()));
        }
        else {
            return null;
        }
    }

    @Override
    public OutputLinkDef tryFindLink(String line) {
        int sepIndex = line.indexOf(':');
        if (sepIndex < 0) {
            return null;
        }

        OutputLinkDef result = tryFindLink(line, sepIndex);
        if (result == null) {
            // Look for another ":" because paths on Windows might contain one
            // in the path. E.g.: "C:\\file"
            sepIndex = line.indexOf(':', sepIndex + 1);
            if (sepIndex < 0) {
                return null;
            }

            return tryFindLink(line, sepIndex);
        }
        return null;
    }
}
