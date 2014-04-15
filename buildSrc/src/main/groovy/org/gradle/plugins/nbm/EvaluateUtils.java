package org.gradle.plugins.nbm;

import groovy.lang.Closure;
import groovy.lang.GString;
import java.io.File;
import java.net.URI;
import java.nio.file.Path;
import java.nio.file.Paths;

public final class EvaluateUtils {
    public static String asString(Object obj) {
        if (obj instanceof Closure) {
            return asString(((Closure<?>)obj).call());
        }

        return obj != null ? obj.toString() : null;
    }

    public static Path asPath(Object obj) {
        if (obj instanceof Path || obj == null) {
            return (Path)obj;
        }
        if (obj instanceof File) {
            return ((File)obj).toPath();
        }
        if (obj instanceof URI) {
            return Paths.get((URI)obj);
        }
        if (obj instanceof String || obj instanceof GString) {
            return Paths.get(obj.toString());
        }
        if (obj instanceof Closure) {
            return asPath(((Closure<?>)obj).call());
        }

        throw new IllegalArgumentException("Unexpected file type: " + obj.getClass().getName());
    }

    private EvaluateUtils() {
        throw new AssertionError();
    }

}
