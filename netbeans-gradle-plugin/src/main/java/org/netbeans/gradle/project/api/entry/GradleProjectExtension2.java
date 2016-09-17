package org.netbeans.gradle.project.api.entry;

import javax.annotation.Nonnull;
import org.openide.util.Lookup;

/**
 * Defines an extension of a particular Gradle project. Instances of this class
 * are expected to be created by a {@link GradleProjectExtensionDef}.
 * <P>
 * Instances of this interface must be safe to be called by multiple threads
 * concurrently but they are not required to be
 * <I>synchronization transparent</I> unless otherwise noted.
 *
 * @param <ModelType> the type of the parsed model storing the information
 *   retrieved from the evaluated build script of the Gradle project
 *
 * @see GradleProjectExtensionDef
 */
public interface GradleProjectExtension2<ModelType> {
    /**
     * Returns the lookup whose content is to be added to the project's lookup
     * at all times regardless if this extension is enabled or not for the associated project.
     * This is equivalent to the {@link org.netbeans.spi.project.ProjectServiceProvider}
     * annotation.
     * <P>
     * Note that in most cases you want to remove objects from the project's
     * lookup if the extension is to be disabled for the project. That is, this
     * extension is not applicable for this project based on the information
     * extracted from the build scripts.
     * <P>
     * To provide instances on the project's lookup only when this extension is
     * enabled, use the {@link #getProjectLookup()} instead.
     * <P>
     * A good example for this lookup is {@link org.netbeans.spi.project.ui.ProjectOpenedHook}.
     *
     * @return the lookup whose content is to be added to the project's lookup
     *   at all times. This method may never return {@literal null}.
     *
     * @see #getProjectLookup()
     * @see #getExtensionLookup()
     */
    @Nonnull
    public Lookup getPermanentProjectLookup();

    /**
     * Returns the lookup whose content is only added to the project's lookup,
     * if this extension is enabled for the associated project.
     * <P>
     * Note that {@link org.netbeans.spi.project.ui.ProjectOpenedHook} should be
     * on the project's lookup at all times, otherwise it might not get notified
     * properly.
     *
     * @return the lookup whose content is only added to the project's lookup,
     *   if this extension is enabled for the associated project. This method
     *   may never return {@literal null}.
     *
     * @see #getPermanentProjectLookup()
     * @see #getExtensionLookup()
     */
    @Nonnull
    public Lookup getProjectLookup();

    /**
     * Returns the lookup whose content is not added to the project's lookup but
     * is used to communicate with the Gradle plugin only.
     * <P>
     * The Gradle plugin recognizes the following types on this lookup:
     * <ul>
     *  <li>{@link org.netbeans.gradle.project.api.config.CustomProfileQuery}</li>
     *  <li>{@link org.netbeans.gradle.project.api.config.InitScriptQuery}</li>
     *  <li>{@link org.netbeans.gradle.project.api.nodes.GradleProjectContextActions}</li>
     *  <li>{@link org.netbeans.gradle.project.api.nodes.GradleProjectExtensionNodes}</li>
     *  <li>{@link org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery}</li>
     *  <li>{@link org.netbeans.gradle.project.api.task.GradleTaskVariableQuery}</li>
     *  <li>
     *   {@link org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsCategory}:
     *   You may add as many instances of this class as you want to the lookup,
     *   if you need more than one project settings page.
     *  </li>
     *  <li>
     *   {@link org.netbeans.gradle.project.api.config.ui.ProfileBasedForeignSettingsCategory}:
     *   You may add as many instances of this class as you want to the lookup. However, consider
     *   using {@code ProfileBasedSettingsCategory} instead.
     *  </li>
     *  <li>
     *   {@link org.netbeans.spi.project.ui.support.ProjectCustomizer.CompositeCategoryProvider}:
     *   You may add as many instances of this interface as you want to the lookup,
     *   if you need more than one customizer. However, it is recommended to provide an instance of
     *   {@code ProfileBasedSettingsCategory} instead.
     *  </li>
     * </ul>
     * <P>
     * Note that the Gradle plugin will look for these types on each of the three
     * lookups of this extension. However, if you don't need to (or don't want to)
     * share instances with NetBeans (and other extensions) as well, you should
     * provide those objects on the lookup returned by this method.
     * <P>
     * <B>Implementation note</B>: If this method ever changes the
     * {@literal Lookup} object it returns, then it must consider listeners
     * registered to the results of the lookup operation (see: {@literal Lookup.Result}).
     * It is however recommended to always return the same lookup instance.
     *
     * @return the lookup whose content is not added to the project's lookup but
     *   is used to communicate with the Gradle plugin only. This method may
     *   never return {@literal null}.
     *
     * @see #getPermanentProjectLookup()
     * @see #getProjectLookup()
     */
    @Nonnull
    public Lookup getExtensionLookup();

    /**
     * Activates this extension from the parsed model retrieved by a previous
     * call to {@link GradleProjectExtensionDef#parseModel(ModelLoadResult) GradleProjectExtensionDef.parseModel}.
     * <P>
     * Prior activating this extension the associated project's lookup is updated
     * to also contain the content of {@link #getProjectLookup()}.
     * <P>
     * This method is never called concurrently with itself nor with the
     * {@link #deactivateExtension() deactivateExtension} method.
     * <P>
     * This method might be called multiple times without a
     * {@code deactivateExtension} between subsequent calls.
     *
     * @param parsedModel the model storing all the information extracted from
     *   the build script of the associated project. This argument cannot be
     *   {@code null}.
     */
    public void activateExtension(@Nonnull ModelType parsedModel);

    /**
     * Deactivates this extension. Deactivating this extension means that this
     * extension is not needed for the associated project. The extension is not
     * required to take any action when being deactivated but it might stop
     * listening for events to improve the overall performance of NetBeans.
     * <P>
     * This method is never called concurrently with itself nor with the
     * {@link #activateExtension(Object) activateExtension} method.
     * <P>
     * This method might be called multiple times without an
     * {@code activateExtension} between subsequent calls.
     * <P>
     * <B>Note</B>: This method is not necessarily called after an
     * {@code activateExtension} method call. That is, you cannot use this
     * method for cleaning up work of {@code activateExtension}.
     */
    public void deactivateExtension();
}
