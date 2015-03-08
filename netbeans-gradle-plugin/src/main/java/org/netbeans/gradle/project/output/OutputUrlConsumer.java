package org.netbeans.gradle.project.output;

import java.net.MalformedURLException;
import java.net.URL;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.awt.HtmlBrowser;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

public final class OutputUrlConsumer implements OutputLinkFinder {
    private static final String[] URL_PREFIXES = new String[]{
        "http://",
        "https://",
        "file://"
    };
    private static OutputLinkDef tryGetUrlWithPrefix(String prefix, String line) {
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

        URL url;
        try {
            url = new URL(StringUtils.stripSeperatorsFromEnd(line.substring(startIndex, endIndex)));
        } catch (MalformedURLException ex) {
            return null;
        }

        return new OutputLinkDef(startIndex, endIndex, getUrlOpenTask(url));
    }

    @Override
    public OutputLinkDef tryFindLink(String line) {
        for (String prefix: URL_PREFIXES) {
            return tryGetUrlWithPrefix(prefix, line);
        }
        return null;
    }

    public static Runnable getUrlOpenTask(final URL url) {
        ExceptionHelper.checkNotNullArgument(url, "url");

        return new Runnable() {
            @Override
            public void run() {
                HtmlBrowser.URLDisplayer.getDefault().showURLExternal(url);
            }
        };
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
