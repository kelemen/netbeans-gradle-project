package org.netbeans.gradle.project.api.config;

import java.util.List;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.task.DaemonTaskContext;

/**
 * Defines a query retrieving additional arguments to be passed to Gradle. The arguments
 * are retrieved before each Gradle command or model loading.
 * <P>
 * Instances of this interface are expected to be found on the lookup of the extension
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 */
public interface GradleArgumentQuery {
    /**
     * Returns the additional arguments to be passed to Gradle.
     * <P>
     * Note: These arguments are specified for Gradle itself and not for the
     * JVM executing Gradle. JVM arguments need to be specified via the
     * {@link #getExtraJvmArgs() getExtraJvmArgs} method.
     *
     * @param context the context for which the arguments are needed. This
     *   argument cannot be {@code null}.
     * @return the additional arguments to be passed to Gradle.
     *   This method may never return {@code null}.
     */
    @Nonnull
    public List<String> getExtraArgs(@Nonnull DaemonTaskContext context);

    /**
     * Returns the additional JVM arguments to be passed to Gradle.
     * These arguments are used to start the JVM process executing Gradle
     * which is executing the Gradle command or loading the models.
     * <P>
     * Note: Users may specify additional JVM arguments in the global
     * settings and these JVM arguments will be added regardless what is
     * specified in this list.
     * <P>
     * <B>Warning</B>: Specifying different JVM arguments for different
     * commands are likely to spawn a new Gradle daemon. Note that the
     * Gradle daemon is a long lived process and by default has a
     * considerable memory footprint. Therefore, spawning new Gradle daemons
     * should be avoided if possible.
     *
     * @param context the context for which the arguments are needed. This
     *   argument cannot be {@code null}.
     * @return the additional JVM arguments to be passed to Gradle.
     *   This method may never return {@code null}.
     */
    @Nonnull
    public List<String> getExtraJvmArgs(@Nonnull DaemonTaskContext context);
}
