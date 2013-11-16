package org.netbeans.gradle.model.api;

/**
 * Defines the base interface for query definition submitted for the Gradle
 * daemon. The basic information required by queries is the class path required
 * to execute the methods extracting the information from the Gradle build.
 */
public interface GradleInfoQuery {
    /**
     * Returns the class path required to execute the methods extracting the
     * information from the Gradle build. This method is expected the return
     * the same class path if returned multiple times.
     * <P>
     * The returned must not contain the <I>Tooling API of Gradle</I>.
     *
     * @return the class path required to execute the methods extracting the
     *   information from the Gradle build. This method may never return
     *   {@code null}.
     */
    public ModelClassPathDef getInfoClassPath();
}
