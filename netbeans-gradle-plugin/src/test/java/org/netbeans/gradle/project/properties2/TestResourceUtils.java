package org.netbeans.gradle.project.properties2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;

final class TestResourceUtils {
    private static String getResourcePath(String relPath) {
        return TestResourceUtils.class.getPackage().getName().replace('.', '/') + "/" + relPath;
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
