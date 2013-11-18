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
 * @see GradleProjectExtensionDef
 */
public interface GradleProjectExtension2 {
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
     *   {@link org.netbeans.spi.project.ui.support.ProjectCustomizer.CompositeCategoryProvider}:
     *   You may add as many instances of this query as you want to the lookup,
     *   if you need more than one customizer.
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
     * Attempts to activate this extension from a previously loaded state. The
     * passed cached model is retrieved from the result of previous invocation
     * of the #loadFromModels(Lookup) method call. An invocation of this method
     * invalidates previous invocation of this method and the
     * {@link #loadFromModels(Lookup) loadFromCache} method.
     * <P>
     * After this method call, this extension must be either disabled or
     * enabled as defined by its return value.
     * If the extension is enabled the returned lookup of the
     * {@link #getProjectLookup()} will be included in the project's lookup
     * otherwise those objects will not be present on the lookup. Also, if an
     * extension is enabled, other extensions might be disabled depending on
     * the return value of {@link GradleProjectExtensionDef#getSuppressedExtensions()}.
     *
     * @param cachedModel the previously loaded model returned by a
     *   {@link #loadFromModels(Lookup)} for this project. This argument cannot
     *   be {@code null}.
     * @return {@code true} if this extension is to be enabled, {@code false}
     *   if it is to be disabled
     *
     * @see #loadFromModels(Lookup)
     */
    public boolean loadFromCache(@Nonnull Object cachedModel);

    /**
     * Called whenever the associated project has been (re)loaded. An invocation
     * of this method invalidates previous invocation of this method and the
     * {@link #loadFromCache(Object) loadFromCache} method. Also the
     * {@code loadFromModels} and the {@code loadFromCache} method may not be
     * called concurrently by multiple threads for the same project. Not even
     * the same methods are called concurrently. That is, two {@code loadFromModels}
     * method call for the same {@code GradleProjectExtension2} will not be
     * done concurrently.
     * <P>
     * After this method call, this extension must be either disabled or
     * enabled as defined by its result: {@link ExtensionLoadResult#isActive()}.
     * If the extension is enabled the returned lookup of the
     * {@link #getProjectLookup()} will be included in the project's lookup
     * otherwise those objects will not be present on the lookup. Also, if an
     * extension is enabled, other extensions might be disabled depending on
     * the return value of {@link GradleProjectExtensionDef#getSuppressedExtensions()}.
     * <P>
     * <B>Caching</B>: This method may (and recommended to) return parsed
     * objects which can be used to load this extension by the {@code loadFromCache}
     * method (a single object / project). In fact this method may return the
     * parsed models for other projects as well.
     * <P>
     * The objects returned in the cache will be deserialized using the
     * {@code ClassLoader} used to load the implementing class of this
     * {@code GradleProjectExtension2}. In future, it might be possible to
     * override this behaviour and deserialize the cached object using
     * other class loader(s).
     *
     * @param models the {@code Lookup} containing the available models
     *   loaded via the Tooling API of Gradle. If a model requested by this
     *   extension is not available on the lookup then it could not be loaded
     *   via the Tooling API. It is also possible that additional models are
     *   available on this lookup but this method must only rely on models, this
     *   extension explicitly requested. This argument cannot be {@code null}.
     * @return the result of the attempt to load this extension for the
     *   associated project. The result may (and should) contain parsed models
     *   which can be used to quickly load this extension for a project without
     *   evaluating the build scripts. This method may never return {@code null}.
     *
     * @see org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery1
     * @see org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery2
     */
    @Nonnull
    public ExtensionLoadResult loadFromModels(@Nonnull Lookup models);
}
