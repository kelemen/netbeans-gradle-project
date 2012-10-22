package org.netbeans.gradle.project.output;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

public final class FileLineConsumer implements SmartOutputHandler.Consumer {
    private static final Logger LOGGER = Logger.getLogger(FileLineConsumer.class.getName());

    private static boolean printLink(String line, File file, String otherInfo, OutputWriter output) {
        int lineIndexSep = otherInfo.indexOf(':');
        int lineNumber = -1;
        if (lineIndexSep > 0) {
            try {
                lineNumber = Integer.parseInt(otherInfo.substring(0, lineIndexSep).trim()) - 1;
            } catch (NumberFormatException ex) {
            }
        }

        OutputListener listener = OpenEditorOutputListener.tryCreateListener(file, lineNumber);
        if (listener == null) {
            LOGGER.log(Level.WARNING, "File displayed in the output disapeared: {0}", file);
            return false;
        }

        try {
            output.println(line, listener, false);
        } catch (IOException ex) {
            LOGGER.log(Level.INFO, "Error while printing line.", ex);
            return false;
        }
        return true;
    }

    private static boolean tryPrintLink(String line, int sepIndex, OutputWriter output) {
        File file = new File(line.substring(0, sepIndex).trim());
        if (file.isFile()) {
            return printLink(line, file, line.substring(sepIndex + 1, line.length()), output);
        }
        return false;
    }

    @Override
    public boolean tryConsumeLine(String line, OutputWriter output) throws IOException {
        int sepIndex = line.indexOf(':');
        if (sepIndex < 0) {
            return false;
        }

        if (!tryPrintLink(line, sepIndex, output)) {
            // Look for another ":" because paths on Windows might contain one
            // in the path. E.g.: "C:\\file"
            sepIndex = line.indexOf(':', sepIndex + 1);
            if (sepIndex < 0) {
                return false;
            }

            return tryPrintLink(line, sepIndex, output);
        }
        return false;
    }
}
