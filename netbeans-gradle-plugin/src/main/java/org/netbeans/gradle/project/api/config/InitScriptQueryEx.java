package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;

/**
 * Defines a query returning an init script to be executed before executing
 * Gradle commands (including built-in commands like build). The init script is
 * passed using the "--init-script" Gradle argument.
 * <P>
 * This interface differs from {@link InitScriptQuery} only by allowing itself
 * to be manually maintained. That is, the users may configure that they
 * want to manually maintain the init scripts provided by implementations of
 * this interface. In the manual maintainence case, the init script file is
 * created in the Gradle's home directory (if it does not already exist).
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 */
public interface InitScriptQueryEx extends InitScriptQuery {
    /**
     * Returns the base file name (file name without extension) to be used
     * when the user wants to manually maintain the init script. Note that this
     * implies that strings returned by this method should be unique and contain
     * only characters valid as a file name. The returned file name should not
     * contain the {@literal ".gradle"} extension.
     * <P>
     * The convention is to start the returned string with {@literal "nb-init-"}.
     *
     * @return the base file name (file name without extension) to be used
     *   when the user wants to manually maintain the init script. This method
     *   may never return {@code null}.
     */
    @Nonnull
    public String getBaseFileName();
}
