package org.netbeans.gradle.project.properties;

import java.text.Collator;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.spi.project.ProjectConfiguration;

public final class NbGradleConfiguration implements ProjectConfiguration {
    public static final NbGradleConfiguration DEFAULT_CONFIG = new NbGradleConfiguration(null);

    private static final Collator STR_CMP = Collator.getInstance(Locale.getDefault());
    private static final Comparator<NbGradleConfiguration> ALPHABETICAL_ORDER = new Comparator<NbGradleConfiguration>() {
        @Override
        public int compare(NbGradleConfiguration o1, NbGradleConfiguration o2) {
            if (DEFAULT_CONFIG.equals(o1)) {
                return DEFAULT_CONFIG.equals(o2) ? 0 : -1;
            }
            if (DEFAULT_CONFIG.equals(o2)) {
                return DEFAULT_CONFIG.equals(o1) ? 0 : 1;
            }

            return STR_CMP.compare(o1.getDisplayName(), o2.getDisplayName());
        }
    };

    private final String profileName;

    public NbGradleConfiguration(String profileName) {
        this.profileName = profileName;
    }

    public static void sortProfiles(NbGradleConfiguration[] profileArray) {
        Arrays.sort(profileArray, ALPHABETICAL_ORDER);
    }

    public static void sortProfiles(List<NbGradleConfiguration> profileList) {
        Collections.sort(profileList, ALPHABETICAL_ORDER);
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
