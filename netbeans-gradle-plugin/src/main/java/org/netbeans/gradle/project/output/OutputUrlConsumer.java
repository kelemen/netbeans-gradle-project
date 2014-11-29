package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.awt.HtmlBrowser;
import org.openide.windows.InputOutput;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;
import org.openide.windows.OutputWriter;

public final class OutputUrlConsumer implements SmartOutputHandler.Consumer {
    private static final Logger LOGGER = Logger.getLogger(OutputUrlConsumer.class.getName());

    private static final String[] URL_PREFIXES = new String[]{
        "http://",
        "https://",
        "file://"
    };
    private static URL tryGetUrlWithPrefix(String prefix, String line) {
        int startIndex = line.indexOf(prefix);
        if (startIndex < 0) {
            return null;
        }

        int lineLength = line.length();
        int endIndex = lineLength;
        for (int i = startIndex + prefix.length(); i < lineLength; i++) {
            if (line.charAt(i) <= ' ') {
                endIndex = i;
                break;
            }
        }

        try {
            return new URL(StringUtils.stripSeperatorsFromEnd(line.substring(startIndex, endIndex)));
        } catch (MalformedURLException ex) {
            return null;
        }
    }

    @Override
    public boolean tryConsumeLine(String line, InputOutput ioParent, OutputWriter output) throws IOException {
        for (String prefix: URL_PREFIXES) {
            final URL url = tryGetUrlWithPrefix(prefix, line);
            if (url != null) {
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

    public static OutputListener getUrlListener(final URL url) {
        ExceptionHelper.checkNotNullArgument(url, "url");

        return new OutputListener() {
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
        };
    }
}
