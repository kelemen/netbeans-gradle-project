package org.netbeans.gradle.project;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.StringWriter;
import java.nio.charset.Charset;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;
import org.openide.filesystems.FileUtil;

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

    public static String getResourceAsString(String resourcePath, Charset encoding) throws IOException {
        ClassLoader classLoader = StringUtils.class.getClassLoader();
        if (classLoader == null) {
            classLoader = ClassLoader.getSystemClassLoader();
        }

        InputStream resource = null;
        try {
            resource = classLoader.getResourceAsStream(resourcePath);
            Reader reader = new InputStreamReader(resource, encoding);

            int bufferSize = 4096;
            StringWriter writer = new StringWriter(bufferSize);

            char[] buffer = new char[bufferSize];
            for (int readCount = reader.read(buffer); readCount > 0; readCount = reader.read(buffer)) {
                writer.write(buffer, 0, readCount);
            }

            return writer.toString();
        } finally {
            if (resource != null) {
                resource.close();
            }
        }
    }

    public static void writeStringToFile(String content, Charset encoding, File file) throws IOException {
        File parent = file.getParentFile();
        if (parent != null) {
            FileUtil.createFolder(parent);
        }

        OutputStream output = null;
        try {
            output = new FileOutputStream(file);
            output.write(content.getBytes(encoding));
        } finally {
            if (output != null) {
                output.close();
            }
        }
    }

    private StringUtils() {
        throw new AssertionError();
    }
}
