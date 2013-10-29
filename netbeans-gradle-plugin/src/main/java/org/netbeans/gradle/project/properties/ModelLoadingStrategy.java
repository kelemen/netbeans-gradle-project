package org.netbeans.gradle.project.properties;

import org.gradle.util.GradleVersion;

// Warning: Never rename instances of this enum because they are used to save
//          the actual configuration.
public enum ModelLoadingStrategy {
    USE_IDEA_MODEL(false),
    BEST_POSSIBLE(true),
    UNSET(false);

    private static final GradleVersion GRADLE_VERSION_1_8_RC_1 = GradleVersion.version("1.8-rc-1");

    private final boolean mayUse18Api;

    private ModelLoadingStrategy(boolean mayUse18Api) {
        this.mayUse18Api = mayUse18Api;
    }

    public boolean canUse18Api(GradleVersion gradleVersion) {
        if (!mayUse18Api) {
            return false;
        }

        return gradleVersion.compareTo(GRADLE_VERSION_1_8_RC_1) >= 0;
    }
}
