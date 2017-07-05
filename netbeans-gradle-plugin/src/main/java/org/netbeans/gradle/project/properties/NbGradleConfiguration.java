package org.netbeans.gradle.project.properties;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ProfileDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.spi.project.ProjectConfiguration;

public final class NbGradleConfiguration implements ProjectConfiguration {
    public static final NbGradleConfiguration DEFAULT_CONFIG = new NbGradleConfiguration(null);

    private static final Comparator<NbGradleConfiguration> ALPHABETICAL_ORDER = (o1, o2) -> {
        if (DEFAULT_CONFIG.equals(o1)) {
            return DEFAULT_CONFIG.equals(o2) ? 0 : -1;
        }
        if (DEFAULT_CONFIG.equals(o2)) {
            return DEFAULT_CONFIG.equals(o1) ? 0 : 1;
        }

        return StringUtils.STR_CMP.compare(o1.getDisplayName(), o2.getDisplayName());
    };

    private final ProfileDef profileDef;

    public NbGradleConfiguration(ProfileDef profileDef) {
        this.profileDef = profileDef;
    }

    public static void sortProfiles(NbGradleConfiguration[] profileArray) {
        Arrays.sort(profileArray, ALPHABETICAL_ORDER);
    }

    public static void sortProfiles(List<NbGradleConfiguration> profileList) {
        Collections.sort(profileList, ALPHABETICAL_ORDER);
    }

    public ProfileKey getProfileKey() {
        return ProfileKey.fromProfileDef(profileDef);
    }

    public ProfileDef getProfileDef() {
        return profileDef;
    }

    public String getProfileGroup() {
        return profileDef != null ? profileDef.getGroupName() : null;
    }

    @Override
    public String getDisplayName() {
        return profileDef != null
                ? profileDef.getDisplayName()
                : NbStrings.getDefaultProfileName();
    }

    @Override
    public String toString() {
        return getDisplayName();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + Objects.hashCode(profileDef);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final NbGradleConfiguration other = (NbGradleConfiguration)obj;
        return Objects.equals(this.profileDef, other.profileDef);
    }
}
