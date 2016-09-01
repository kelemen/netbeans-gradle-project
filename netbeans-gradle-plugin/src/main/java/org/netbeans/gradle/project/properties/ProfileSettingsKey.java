package org.netbeans.gradle.project.properties;

import java.util.List;
import javax.annotation.Nonnull;

public interface ProfileSettingsKey {
    @Nonnull
    public List<ProfileSettingsKey> getWithFallbacks();

    /**
     * Opens the profile file associated with this key and allows the reading of its properties.
     * <P>
     * <B>Warning: Do not directly call this method!</B> If you directly call this method, you risk
     * to see inconsistent values with code opened the profile elsewhere. You should always open
     * a profile through {@link ProfileSettingsContainer} which ensures that if you get the same
     * profile multiple times, you will see consistent values.
     *
     * @return the yet to be loaded profile settings associated with this key. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public LoadableSingleProfileSettingsEx openUnloadedProfileSettings();
}
