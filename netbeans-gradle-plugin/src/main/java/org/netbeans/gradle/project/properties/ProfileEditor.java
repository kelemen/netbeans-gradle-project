package org.netbeans.gradle.project.properties;

import javax.annotation.Nonnull;

public interface ProfileEditor {
    @Nonnull
    public StoredSettings readFromSettings();

    @Nonnull
    public StoredSettings readFromGui();
}
