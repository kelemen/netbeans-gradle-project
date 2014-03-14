package org.netbeans.gradle.project.properties2;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.LineNumberReader;
import java.io.Reader;
import java.net.URL;
import java.util.LinkedList;
import java.util.List;

final class TestResourceUtils {
    private static String getResourcePath(String relPath) {
        return TestResourceUtils.class.getPackage().getName().replace('.', '/') + "/" + relPath;
    }

    public static String[] getResourceLines(String relPath, String encoding) throws IOException {
        List<String> lines = new LinkedList<>();
        try (InputStream input = openResource(relPath);
                Reader reader = new InputStreamReader(input, encoding);
                LineNumberReader lineReader = new LineNumberReader(reader, 8 * 1024)) {

            String line = lineReader.readLine();
            while (line != null) {
                lines.add(line);
                line = lineReader.readLine();
            }
        }
        return lines.toArray(new String[lines.size()]);
    }

    public static InputStream openResource(String relPath) throws IOException {
        String absolutePath = getResourcePath(relPath);
        ClassLoader classLoader = ProfileSettingsTest.class.getClassLoader();

        URL url = classLoader.getResource(absolutePath);
        if (url == null) {
            throw new IOException("No URL for resource: " + absolutePath);
        }

        InputStream result = classLoader.getResourceAsStream(absolutePath);
        if (result == null) {
            throw new IOException("Failed to open resource: " + absolutePath);
        }
        return result;
    }

    private TestResourceUtils() {
        throw new AssertionError();
    }
}
