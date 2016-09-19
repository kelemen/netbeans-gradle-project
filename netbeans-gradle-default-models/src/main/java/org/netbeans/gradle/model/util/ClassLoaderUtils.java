package org.netbeans.gradle.model.util;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;

public final class ClassLoaderUtils {
    private static final AtomicReference<File> JAR_OF_THIS_PROJECT = new AtomicReference<File>(null);

    public static File findClassPathOfClass(Class<?> cl) {
        return BasicFileUtils.toCanonicalFile(findClassPathOfClassNonCanonical(cl));
    }

    private static File findClassPathOfClassNonCanonical(Class<?> cl) {
        URL urlOfClassPath = cl.getProtectionDomain().getCodeSource().getLocation();
        if (urlOfClassPath == null) {
            throw new IllegalArgumentException("Unable to locate classpath of " + cl);
        }

        return extractPathFromURL(urlOfClassPath);
    }

    private static File urlToFile(URL url) {
        try {
            return new File(url.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    public static File extractPathFromURL(URL url) {
        String protocol = url.getProtocol();
        if ("jar".equals(protocol)) {
            String[] pathParts = url.getPath().split("!", 2);
            try {
                URL jarURL = new URL(pathParts[0]);
                return urlToFile(jarURL);
            } catch (MalformedURLException ex) {
                throw new IllegalArgumentException("Unexpected URL: " + url);
            }
        }
        else if ("file".equals(protocol)) {
            return urlToFile(url);
        }
        else {
            throw new IllegalArgumentException("Unexpected URL: " + url);
        }
    }

    public static File getLocationOfClassPath() {
        File result = JAR_OF_THIS_PROJECT.get();
        if (result == null) {
            JAR_OF_THIS_PROJECT.set(findClassPathOfClass(ClassLoaderUtils.class));
            result = JAR_OF_THIS_PROJECT.get();
        }
        return result;
    }

    public static ClassLoader classLoaderFromClassPath(Collection<File> classPath, ClassLoader parent) {
        List<URL> urls = new ArrayList<URL>(classPath.size());
        try {
            for (File file: classPath) {
                urls.add(file.toURI().toURL());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }

        return new URLClassLoader(urls.toArray(new URL[urls.size()]), parent);
    }

    private ClassLoaderUtils() {
        throw new AssertionError();
    }
}
