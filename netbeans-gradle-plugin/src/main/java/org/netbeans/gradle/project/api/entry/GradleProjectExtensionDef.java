package org.netbeans.gradle.project.api.entry;

import java.io.IOException;
import java.util.Set;
import javax.annotation.Nonnull;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;

/**
 * Defines a query for extending Gradle based projects. This query must be used
 * by other plugins to integrate with the Gradle project type. That is,
 * instances of {@code GradleProjectExtensionDef} are notified upon each
 * project load.
 * <P>
 * Instances of {@code GradleProjectExtensionDef} are expected to be installed
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
public interface GradleProjectExtensionDef {
    /**
     * Returns the unique name of this extension. The name is used to reference
     * the extension when it conflicts with another extension. That is, the
     * {@link #modelsLoaded(Lookup) modelsLoaded} method may return the name of
     * an extension to suppress it. The recommended naming convention is to name
     * extensions by their fully-qualified class name.
     *
     * @return the unique name of this extension. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public String getName();

    /**
     * Returns the name of this extension as to be displayed to the users.
     * <P>
     * Note: This information is not currently used but might be used for
     * displaying the active extensions to NetBeans users.
     *
     * @return the name of this extension as to be displayed to the users. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public String getDisplayName();

    /**
     * Returns the lookup containing information about this extension
     * independently of projects.
     * <P>
     * The following services are known and used by the Gradle plugin:
     * <ul>
     *  <li>{@link org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery1}</li>
     *  <li>{@link org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery2}</li>
     * </ul>
     *
     * @return the lookup containing information about this extension
     *   independently of projects. This method may never return {@code null}.
     */
    @Nonnull
    public Lookup getLookup();

    /**
     * Attaches the extension to a particular project which has just been
     * loaded. This method is called for each loaded project and is called
     * exactly once.
     * <P>
     * <B>Note</B>: When this method is called, the lookup of the project is
     * incomplete and callers of the lookup must not expect to be notified
     * when the lookup of the project changes (via {@code Lookup.Result}).
     * <P>
     * You can expect the following queries to be on the project's lookup even
     * when this method is being called:
     * <ul>
     *  <li>{@link org.netbeans.spi.project.ProjectState}</li>
     *  <li>{@link org.netbeans.api.project.ProjectInformation}</li>
     *  <li>{@link org.netbeans.spi.project.AuxiliaryConfiguration}</li>
     *  <li>{@link org.netbeans.spi.project.AuxiliaryProperties}</li>
     *  <li>{@link org.netbeans.gradle.project.api.task.GradleCommandExecutor}</li>
     *  <li>{@link org.netbeans.gradle.project.api.property.GradleProperty.SourceEncoding}</li>
     *  <li>{@link org.netbeans.gradle.project.api.property.GradleProperty.ScriptPlatform}</li>
     *  <li>{@link org.netbeans.gradle.project.api.property.GradleProperty.SourceLevel}</li>
     *  <li>{@link org.netbeans.gradle.project.api.property.GradleProperty.BuildPlatform}</li>
     * </ul>
     *
     * @param project the project which has been loaded and to which this
     *   extension is to be attached. This argument cannot be {@code null}.
     * @return the {@code GradleProjectExtension2} handling the extension of the
     *   loaded project. This method may never return {@code null}.
     *
     * @throws IOException thrown if some serious I/O error prevented the
     *   extension from being loaded. Throwing this exception will prevent the
     *   extension from being applied to this project (even after it changes),
     *   so this exception should only be thrown in the extreme cases.
     */
    @Nonnull
    public GradleProjectExtension2 createExtension(@Nonnull Project project) throws IOException;

    /**
     * Returns the {@link #getName() names} of the extensions to be disabled
     * if this extension considers itself to be active for a project. This
     * method is expected to return the same set of names each time it is called.
     * <P>
     * Projects that are not Java projects usually need to suppress the Java
     * extension whose name is "org.netbeans.gradle.project.java.JavaExtension".
     *
     * @return the {@link #getName() names} of the extensions to be disabled
     *   if this extension considers itself to be active for a project. This
     *   method may never return {@code null} and the elements of the returned
     *   set cannot be {@code null}.
     */
    @Nonnull
    public Set<String> getSuppressedExtensions();
}
