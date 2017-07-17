package org.netbeans.gradle.project.output;

import java.io.File;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class RegexpFileLineConsumer implements OutputLinkFinder {
    private static final Logger LOGGER = Logger.getLogger(RegexpFileLineConsumer.class.getName());

    private static final Pattern FILE_LINE_PATTERN = Pattern.compile(
            "\\s*((.*[/\\\\]+.+):(\\d+)):\\s+(?:error|warning):\\s[^\\s]+.*"
    );

    @Override
    public OutputLinkDef tryFindLink(String line) {

        Matcher matcher = FILE_LINE_PATTERN.matcher(line);
        if (!matcher.find()) {
            return null;
        }
        if (matcher.groupCount() != 3) {
            return null;
        }

        int lineNumber;
        try {
            lineNumber = Integer.parseInt(matcher.group(3));
        } catch (NumberFormatException ex) {
            // should not be possible
            return null;
        }

        String path = matcher.group(2).trim();
        File file;
        try {
            file = new File(path);
            if (!file.isFile()) {
                return null;
            }
        } catch (Exception ex) {
            LOGGER.log(Level.FINE, "Path displayed in the output is not readable: {0}", path);
            return null;
        }

        return createLink(line, file, path, lineNumber, matcher.group(1).length());
    }

    private static OutputLinkDef createLink(String line, File file, String path, int lineNumber, int linkLength) {
        Runnable listener = OpenEditorOutputListener.tryCreateListener(file, lineNumber);
        if (listener == null) {
            LOGGER.log(Level.FINE, "File displayed in the output disappeared: {0}", file);
            return null;
        }

        int startIndex = line.indexOf(path);
        return new OutputLinkDef(startIndex, startIndex + linkLength, listener);
    }

}
