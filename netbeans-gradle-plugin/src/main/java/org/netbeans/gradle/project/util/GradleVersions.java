package org.netbeans.gradle.project.util;

import org.gradle.util.GradleVersion;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.openide.modules.SpecificationVersion;

public final class GradleVersions {
    /**
     * This should only be used as a fallback when there was no known target
     * but we must have something regardless.
     */
    public static final GradleTarget DEFAULT_TARGET
            = new GradleTarget(getDefaultSpecVersion(), GradleVersion.version("1.0"));

    public static final GradleVersion VERSION_1_7 = GradleVersion.version("1.7");
    public static final GradleVersion VERSION_1_8_RC_1 = GradleVersion.version("1.8-rc-1");
    public static final GradleVersion VERSION_1_8 = GradleVersion.version("1.8");
    public static final GradleVersion VERSION_2_3 = GradleVersion.version("2.3");

    private static SpecificationVersion getDefaultSpecVersion() {
        Specification spec = JavaPlatform.getDefault().getSpecification();
        SpecificationVersion result = spec != null ? spec.getVersion() : null;

        return result != null ? result : new SpecificationVersion("1.5");
    }

    private GradleVersions() {
        throw new AssertionError();
    }
}
