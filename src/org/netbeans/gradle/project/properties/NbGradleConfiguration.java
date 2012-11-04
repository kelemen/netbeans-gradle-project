package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.NbStrings;
import org.netbeans.spi.project.ProjectConfiguration;

public final class NbGradleConfiguration implements ProjectConfiguration {
    public static final NbGradleConfiguration DEFAULT_CONFIG = new NbGradleConfiguration(null);

    private final String profileName;

    public NbGradleConfiguration(String profileName) {
        this.profileName = profileName;
    }

    public String getProfileName() {
        return profileName;
    }

    @Override
    public String getDisplayName() {
        return profileName != null
                ? profileName
                : NbStrings.getDefaultProfileName();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.profileName != null ? this.profileName.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        final NbGradleConfiguration other = (NbGradleConfiguration)obj;
        if ((this.profileName == null) ? (other.profileName != null) : !this.profileName.equals(other.profileName))
            return false;
        return true;
    }
}
