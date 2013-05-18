package org.netbeans.gradle.project.api.entry;

import java.io.IOException;
import org.netbeans.api.project.Project;

/**
 * Defines a query for extending Gradle based projects. This query must be used
 * by other plugins to integrate with the Gradle project type. That is,
 * instances if {@code GradleProjectExtensionQuery} are notified upon each
 * project load.
 * <P>
 * Instances of {@code GradleProjectExtensionQuery} are expected to be installed
 * into the global default lookup: {@code org.openide.util.Lookup.getDefault()}
 * via the {@code org.openide.util.lookup.ServiceProvider} annotation.
 * <P>
 * <B>Note</B>: As of the current implementation, this query must be on the
 * global lookup prior loading the project. This means that plugins integrating
 * with Gradle projects require NetBeans to be restarted after being installed.
 * <P>
 * Instances of this interface must be safe to be called by multiple threads
 * concurrently but they are not required to be
 * <I>synchronization transparent</I> unless otherwise noted.
 */
public interface GradleProjectExtensionQuery {
    /**
     * Attaches the extension to a particular project which has just been
     * loaded. This method is called for each loaded project and is called
     * exactly once.
     * <P>
     * <B>Note</B>: When this method is called, the lookup of the project is
     * incomplete and callers of the lookup must not expect to be notified
     * when the lookup of the project changes (via {@code Lookup.Result}).
     *
     * @param project the project which has been loaded and to which this
     *   extension is to be attached. This argument cannot be {@code null}.
     * @return the {@code GradleProjectExtension} handling the extension of the
     *   loaded project. This method may never return {@code null}.
     *
     * @throws IOException thrown if some serious I/O error prevented the
     *   extension from being loaded. Throwing this exception will prevent the
     *   extension from being applied to this project (even after it changes),
     *   so this exception should only be thrown in the extreme cases.
     *
     */
    public GradleProjectExtension loadExtensionForProject(Project project) throws IOException;
}
