package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;

/**
 * Defines a provider for the global settings associated with an extension of this Gradle plugin.
 * That is, this provider lets you access the {@link ProfileKey#GLOBAL_PROFILE default global profile}
 * without an associated project. Note that all profiles will eventually inherit from this global profile,
 * so if you have the same definition for a project property
 * (in {@link ProjectSettingsProvider ProjectSettingsProvider}) and in the global profile, they will be
 * merged as defined by the property.
 * <P>
 * An instance of this provider is can be retrieved through
 * {@link GlobalConfig#getDefault() GlobalConfig.getDefault()}.
 */
public interface GlobalSettingsProvider {
    /**
     * Returns the global settings for a particular extension. The extensions
     * must be identified by a globally unique string which is preferably the
     * name of the extension as defined by
     * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef GradleProjectExtensionDef}.
     * Note that this name should also be the same as provided for {@link ProjectSettingsProvider},
     * otherwise project properties will fail to inherit from the global properties.
     *
     * @param extensionName the string identifying the extension in the
     *   configuration. The preferred convention is to use the extension's name,
     *   though it is not strictly required. This argument cannot be {@code null}.
     * @return the global settings of the requested extension. This method
     *   never returns {@code null}.
     */
    @Nonnull
    public ActiveSettingsQuery getExtensionSettings(@Nonnull String extensionName);
}
