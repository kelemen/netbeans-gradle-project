package org.netbeans.gradle.project.properties;

import org.gradle.util.GradleVersion;
import org.netbeans.gradle.project.GradleVersions;

// Warning: Never rename instances of this enum because they are used to save
//          the actual configuration.
public enum ModelLoadingStrategy {
    NEWEST_POSSIBLE(true),
    USE_IDEA_MODEL(false);

    private final boolean mayUse18Api;

    private ModelLoadingStrategy(boolean mayUse18Api) {
        this.mayUse18Api = mayUse18Api;
    }

    public boolean canUse18Api(GradleVersion gradleVersion) {
        if (!mayUse18Api) {
            return false;
        }

        return gradleVersion.compareTo(GradleVersions.VERSION_1_8_RC_1) >= 0;
    }
}
