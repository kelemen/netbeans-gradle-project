package org.netbeans.gradle.project.api.config;

import java.util.Collection;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim2.cancel.CancellationToken;

/**
 * Defines a provider for the project settings of extensions. Unlike the
 * <I>NetBeans</I> API
 * ({@link org.netbeans.spi.project.AuxiliaryProperties AuxiliaryProperties} and
 * {@link org.netbeans.spi.project.AuxiliaryConfiguration AuxiliaryConfiguration}),
 * this query lets you access profile specific settings.
 * <P>
 * This {@code ProjectSettingsProvider} is available on the
 * {@link org.netbeans.api.project.Project#getLookup() project's lookup}. It is
 * already available when loading extensions (i.e., maybe retrieved in the
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#createExtension(org.netbeans.api.project.Project) GradleProjectExtensionDef.createExtension}
 * method.
 *
 * @see ProfileDef
 * @see PropertyReference
 */
public interface ProjectSettingsProvider {
    /**
     * Returns the list of all the available profiles except for the default
     * profile.
     *
     * @return the list of all the available profiles. This method never returns
     *   {@code null} and none of the elements of the returned collection is
     *   {@code null}.
     */
    @Nonnull
    public Collection<ProfileDef> getCurrentProfileDefs();

    /**
     * Returns the project settings of a particular extension. The extensions
     * must be identified by a globally unique string which is preferably the
     * name of the extension as defined by
     * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef GradleProjectExtensionDef}.
     *
     * @param extensionName the string identifying the extension in the
     *   configuration. The preferred convention is to use the extension's name,
     *   though it is not strictly required. This argument cannot be {@code null}.
     * @return the project settings of the requested extension. This method
     *   never returns {@code null}.
     */
    @Nonnull
    public ExtensionSettings getExtensionSettings(@Nonnull String extensionName);

    /**
     * Defines the project settings of a particular extension.
     *
     * @see ProjectSettingsProvider#getExtensionSettings(String)
     */
    public static interface ExtensionSettings {
        /**
         * Returns the settings based on the profile currently selected in the
         * IDE. That is, these settings should be used by actions which require
         * the project properties.
         * <P>
         * Note: There is no reason to request the settings provided by this
         * method multiple times. That is, any changes will be reflected in the
         * returned {@code ActiveSettingsQuery}.
         *
         * @return the settings based on the profile currently selected in the
         *   IDE. This method never returns {@code null}.
         */
        @Nonnull
        public ActiveSettingsQuery getActiveSettings();

        /**
         * Returns the settings for the specified profile. This method will wait
         * until the selected profile and all of its fallback profiles were
         * completely loaded.
         *
         * @param cancelToken the {@code CancellationToken} which might signal
         *   that this operation can be canceled. Such request might be ignored,
         *   if it is not an {@code OperationCanceledException} is thrown.
         *   This argument cannot be {@code null}.
         * @param profile the {@code ProfileKey} identifying the profile to
         *   be requested. This argument can be {@code null} to request the
         *   default profile.
         * @return the settings for the specified profile. This method never
         *   returns {@code null}.
         *
         * @throws org.jtrim2.cancel.OperationCanceledException thrown if
         *   cancellation was requested and this operation was canceled
         */
        @Nonnull
        public ActiveSettingsQuery loadSettingsForProfile(
                @Nonnull CancellationToken cancelToken,
                @Nullable ProfileKey profile);

        /**
         * Returns the settings for the specified profile asynchronously. This
         * method will return immediately and the {@code ActiveSettingsQuery}
         * will be passed to the specified {@code ActiveSettingsQueryListener}.
         *
         * @param cancelToken the {@code CancellationToken} which might signal
         *   that this operation can be canceled. Such request might be ignored,
         *   if it is not, the specified listener will never be called. This
         *   argument cannot be {@code null}.
         * @param profile the {@code ProfileKey} identifying the profile to
         *   be requested. This argument can be {@code null} to request the
         *   default profile.
         * @param settingsQueryListener the listener to be notified when the
         *   requested profile was completely loaded. This argument cannot be
         *   {@code null}.
         */
        @Nonnull
        public void loadSettingsForProfile(
                @Nonnull CancellationToken cancelToken,
                @Nullable ProfileKey profile,
                @Nonnull ActiveSettingsQueryListener settingsQueryListener);
    }
}
