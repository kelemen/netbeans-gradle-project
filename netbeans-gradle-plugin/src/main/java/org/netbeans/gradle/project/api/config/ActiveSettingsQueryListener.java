package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;

/**
 * Defines a listener to receive asynchronously loaded {@code ActiveSettingsQuery}.
 *
 * @see ProjectSettingsProvider.ExtensionSettings
 */
public interface ActiveSettingsQueryListener {
    /**
     * Called when the given {@code ActiveSettingsQuery} was already loaded.
     * Note that it is usually undefined on what thread this method is called.
     *
     * @param settings the loaded settings. This argument cannot be {@code null}.
     */
    public void onLoad(@Nonnull ActiveSettingsQuery settings);
}
