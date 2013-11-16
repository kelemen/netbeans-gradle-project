package org.netbeans.gradle.project.api.modelquery;

import javax.annotation.Nonnull;
import org.gradle.util.GradleVersion;
import org.openide.modules.SpecificationVersion;

/**
 * Defines the properties of the target Gradle daemon.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * by multiple threads concurrently.
 */
public final class GradleTarget {
    private final SpecificationVersion jdkVersion;
    private final GradleVersion gradleVersion;

    /**
     * Creates a new {@code GradleTarget} with the given JRE and Gradle version.
     *
     * @param javaVersion the version of JRE used to execute the Gradle daemon.
     *   This argument cannot be {@code null}.
     * @param gradleVersion the version of Gradle used in the Gradle daemon.
     *   This argument cannot be {@code null}.
     */
    public GradleTarget(
            @Nonnull SpecificationVersion javaVersion,
            @Nonnull GradleVersion gradleVersion) {
        if (javaVersion == null) throw new NullPointerException("javaVersion");
        if (gradleVersion == null) throw new NullPointerException("gradleVersion");

        this.jdkVersion = javaVersion;
        this.gradleVersion = gradleVersion;
    }

    /**
     * Returns the version of JRE (or JDK) used to execute the Gradle daemon.
     *
     * @return the version of Java used to execute the Gradle daemon. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public SpecificationVersion getJavaVersion() {
        return jdkVersion;
    }

    /**
     * Returns the version of Gradle used in the Gradle daemon.
     *
     * @return the version of Gradle used in the Gradle daemon.
     */
    @Nonnull
    public GradleVersion getGradleVersion() {
        return gradleVersion;
    }
}
