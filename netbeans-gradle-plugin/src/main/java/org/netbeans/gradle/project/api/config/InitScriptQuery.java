package org.netbeans.gradle.project.api.config;

import java.io.IOException;
import javax.annotation.Nonnull;

/**
 * Defines a query returning an init script to be executed before executing
 * Gradle commands (including built-in commands like build). The init script is
 * passed using the "--init-script" Gradle argument.
 * <P>
 * <B>It is recommended that init scripts only contain ASCII characters.</B>
 * <P>
 * It is extremely important for init scripts to be defensively coded, so that
 * they do not interfere with the user's build script. That is, if the init
 * script breaks the build, users may be forced to disable the init script
 * completely.
 * <P>
 * <B>Note</B>: You should consider implementing {@link InitScriptQueryEx} instead.
 * <P>
 * Instances of this interface are expected to be found on the lookup of the extension
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtension2#getExtensionLookup() (getExtensionLookup)}.
 * <P>
 * Instances of this interface are required to be safe to be accessed by
 * multiple threads concurrently.
 *
 * @see InitScriptQueryEx
 */
public interface InitScriptQuery {
    /**
     * Returns the content of the init script to be executed before each Gradle
     * command. The returned init script must be coded as defensively as
     * possible. The script is passed using the "--init-script" argument of
     * Gradle.
     * <P>
     * <B>It is recommended that init scripts only contain ASCII characters.</B>
     *
     * @return the content of the init script to be executed before each Gradle
     *   command. This method may never return {@code null}.
     *
     * @throws IOException thrown if the init-script could not be fetched for
     *   some reason. Throwing this exception will prevent this init script from
     *   being executed (obviously). Throwing this exception is considered a
     *   severe error.
     */
    @Nonnull
    public String getInitScript() throws IOException;
}
