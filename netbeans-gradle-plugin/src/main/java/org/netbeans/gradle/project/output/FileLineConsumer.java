package org.netbeans.gradle.project.output;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class FileLineConsumer implements OutputLinkFinder {
    private static final Logger LOGGER = Logger.getLogger(FileLineConsumer.class.getName());

    private static int trimEndIndex(String str, int endIndex) {
        for (int i = endIndex - 1; i >= 0; i--) {
            if (str.charAt(i) > ' ') {
                return i + 1;
            }
        }
        return 0;
    }

    private static OutputLinkDef tryFindLink(String line, File file, int otherInfoStartIndex) {
        String otherInfo = line.substring(otherInfoStartIndex, line.length());
        int endIndex = otherInfoStartIndex - 1;

        int lineIndexSep = otherInfo.indexOf(':');
        int lineNumber = -1;
        if (lineIndexSep > 0) {
            try {
                lineNumber = Integer.parseInt(otherInfo.substring(0, lineIndexSep).trim());
                endIndex = otherInfoStartIndex + lineIndexSep;
            } catch (NumberFormatException ex) {
            }
        }

        Runnable listener = OpenEditorOutputListener.tryCreateListener(file, lineNumber);
        if (listener == null) {
            LOGGER.log(Level.WARNING, "File displayed in the output disappeared: {0}", file);
            return null;
        }

        return new OutputLinkDef(0, trimEndIndex(line, endIndex), listener);
    }

    private OutputLinkDef tryFindLink(String line, int sepIndex) {
        String fileStr = line.substring(0, sepIndex).trim();
        File file = new File(fileStr);
        if (file.isFile()) {
            return tryFindLink(line, file, sepIndex + 1);
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
