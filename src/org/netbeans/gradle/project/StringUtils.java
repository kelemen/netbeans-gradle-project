package org.netbeans.gradle.project;

import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

public final class StringUtils {
    public static String[] splitText(String text, String delimiters) {
        StringTokenizer tokenizer = new StringTokenizer(text, delimiters);
        List<String> result = new LinkedList<String>();
        while (tokenizer.hasMoreTokens()) {
            String token = tokenizer.nextToken().trim();
            if (!token.isEmpty()) {
                result.add(token);
            }
        }

        return result.toArray(new String[result.size()]);
    }

    public static String[] splitLines(String text) {
        return splitText(text, "\n\r");
    }

    public static String[] splitBySpaces(String text) {
        return splitText(text, " \t\n\r\f");
    }

    private StringUtils() {
        throw new AssertionError();
    }
}
