package org.netbeans.gradle.project.util;

import java.util.Arrays;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Closeables {
    private static final Logger LOGGER = Logger.getLogger(Closeables.class.getName());

    public static void closeAll(AutoCloseable... resources) {
        closeAll(Arrays.asList(resources));
    }

    public static void closeAll(Collection<? extends AutoCloseable> resources) {
        for (AutoCloseable resource: resources) {
            try {
                if (resource != null) {
                    resource.close();
                }
            } catch (Throwable ex) {
                String resourceStr = "?";
                try {
                    resourceStr = String.valueOf(resource);
                } catch (Throwable subEx) {
                    ex.addSuppressed(subEx);
                }

                LOGGER.log(Level.SEVERE, "Failed to close resource: " + resourceStr, ex);
            }
        }
    }

    private Closeables() {
        throw new AssertionError();
    }
}
