package org.netbeans.gradle.project;

import org.gradle.util.GradleVersion;

public final class GradleVersions {
    public static final GradleVersion VERSION_1_7 = GradleVersion.version("1.7");
    public static final GradleVersion VERSION_1_8_RC_1 = GradleVersion.version("1.8-rc-1");
    public static final GradleVersion VERSION_1_8 = GradleVersion.version("1.8");

    private GradleVersions() {
        throw new AssertionError();
    }
}
