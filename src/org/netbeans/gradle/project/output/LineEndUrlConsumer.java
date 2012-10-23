package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import org.openide.awt.HtmlBrowser;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

public final class LineEndUrlConsumer implements SmartOutputHandler.Consumer {
    private static final Logger LOGGER = Logger.getLogger(LineEndUrlConsumer.class.getName());

    private static final String[] URL_PREFIXES = new String[]{
        "http://",
        "https://",
        "file://"
    };
    private static final String SEPARATORS = ",./?;:'\"";

    private static String stripSeperatorsFromEnd(String str) {
        for (int i = str.length() - 1; i >= 0; i--) {
            if (SEPARATORS.indexOf(str.charAt(i)) < 0) {
                return str.substring(0, i + 1);
            }
        }
        return "";
    }

    @Override
    public boolean tryConsumeLine(String line, OutputWriter output) throws IOException {
        for (String prefix: URL_PREFIXES) {
            int startIndex = line.lastIndexOf(prefix);
            if (startIndex >= 0) {
                final URL url;
                try {
                    url = new URL(stripSeperatorsFromEnd(line.substring(startIndex)));
                } catch (MalformedURLException ex) {
                    return false;
                }

                output.println(line, new OutputListener() {
                    @Override
                    public void outputLineSelected(OutputEvent ev) {
                    }

                    @Override
                    public void outputLineAction(OutputEvent ev) {
                        HtmlBrowser.URLDisplayer.getDefault().showURLExternal(url);
                    }

                    @Override
                    public void outputLineCleared(OutputEvent ev) {
                    }
                }, false);
                return true;
            }
        }
        return false;
    }
}
