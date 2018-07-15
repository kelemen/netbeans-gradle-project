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
    private static final AtomicReference<URL> JAR_OF_THIS_PROJECT = new AtomicReference<URL>(null);
    private static final String JAR_PATH_TERMINATOR = "!/";

    /**
     * @deprecated Use {@link #findUrlClassPathOfClass(Class) findUrlClassPathOfClass}
     *
     * @param cl class whose class path is to be returned
     * @return the JAR containing the class
     */
    @Deprecated
    public static File findClassPathOfClass(Class<?> cl) {
        return BasicFileUtils.toCanonicalFile(extractPathFromURL(findUrlClassPathOfClass(cl)));
    }

    public static URL findUrlClassPathOfClass(Class<?> cl) {
        URL urlOfClassPath = cl.getProtectionDomain().getCodeSource().getLocation();
        if (urlOfClassPath == null) {
            throw new IllegalArgumentException("Unable to locate classpath of " + cl);
        }

        return extractFileUrlFromUrl(urlOfClassPath);
    }

    private static File urlToFile(URL url) {
        try {
            return new File(url.toURI());
        } catch (URISyntaxException ex) {
            throw new IllegalArgumentException(ex);
        }
    }

    private static String extractRawPathFromJarUrl(URL url) {
        String path = url.getPath();
        if (path.endsWith(JAR_PATH_TERMINATOR)) {
            return path.substring(0, path.length() - JAR_PATH_TERMINATOR.length());
        }

        int jarPathEnd = path.indexOf(JAR_PATH_TERMINATOR);
        return path.substring(0, jarPathEnd);
    }

    private static URL extractUrlFromJarUrl(URL url) {
        try {
            return new URL(extractRawPathFromJarUrl(url));
        } catch (MalformedURLException ex) {
            throw new IllegalArgumentException("Unexpected URL: " + url, ex);
        }
    }

    public static URL extractFileUrlFromUrl(URL url) {
        String protocol = url.getProtocol();
        if ("jar".equals(protocol)) {
            return extractUrlFromJarUrl(url);
        }
        else{
            return url;
        }
    }

    /**
     * @deprecated Use {@link #extractFileUrlFromUrl(URL) extractFileUrlFromUrl}
     *
     * @param url URL from which the file object is to be returned
     * @return the file to which the URL points to
     */
    @Deprecated
    public static File extractPathFromURL(URL url) {
        URL fileUrl = extractFileUrlFromUrl(url);
        if ("file".equals(fileUrl.getProtocol())) {
            return urlToFile(fileUrl);
        }
        else {
            throw new IllegalArgumentException("Unexpected URL: " + url);
        }
    }

    public static URL getUrlOfClassPath() {
        URL result = JAR_OF_THIS_PROJECT.get();
        if (result == null) {
            JAR_OF_THIS_PROJECT.set(findUrlClassPathOfClass(ClassLoaderUtils.class));
            result = JAR_OF_THIS_PROJECT.get();
        }
        return result;
    }

    /**
     * @deprecated Use {@link #getUrlOfClassPath() getUrlOfClassPath}
     *
     * @return the class path entry containing this class
     */
    @Deprecated
    public static File getLocationOfClassPath() {
        return BasicFileUtils.toCanonicalFile(extractPathFromURL(getUrlOfClassPath()));
    }

    public static ClassLoader classLoaderFromClassPathUrls(Collection<URL> classPath, ClassLoader parent) {
        return new URLClassLoader(classPath.toArray(new URL[classPath.size()]), parent);
    }

    /**
     * @deprecated Use {@link #classLoaderFromClassPathUrls(Collection, ClassLoader) classLoaderFromClassPath}
     *
     * @param classPath the new class path entries
     * @param parent the parent class loader to delegate to
     * @return the class loader loading the given class path
     */
    @Deprecated
    public static ClassLoader classLoaderFromClassPath(Collection<File> classPath, ClassLoader parent) {
        List<URL> urls = new ArrayList<URL>(classPath.size());
        try {
            for (File file: classPath) {
                urls.add(file.toURI().toURL());
            }
        } catch (MalformedURLException ex) {
            throw new RuntimeException(ex);
        }
        return classLoaderFromClassPathUrls(urls, parent);
    }

    private ClassLoaderUtils() {
        throw new AssertionError();
    }
}
