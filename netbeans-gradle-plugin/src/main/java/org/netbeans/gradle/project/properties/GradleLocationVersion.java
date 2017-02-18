package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.tasks.vars.StringResolver;

public final class GradleLocationVersion implements GradleLocation {
    public static final String UNIQUE_TYPE_NAME = "VER";

    private final String versionStr;

    public GradleLocationVersion(String versionStr) {
        ExceptionHelper.checkNotNullArgument(versionStr, "versionStr");
        this.versionStr = versionStr;
    }

    public static GradleLocationRef getLocationRef(final String rawVersionStr) {
        ExceptionHelper.checkNotNullArgument(rawVersionStr, "rawVersionStr");

        return new GradleLocationRef() {
            @Override
            public String getUniqueTypeName() {
                return UNIQUE_TYPE_NAME;
            }

            @Override
            public String asString() {
                return rawVersionStr;
            }

            @Override
            public GradleLocation getLocation(StringResolver resolver) {
                return new GradleLocationVersion(resolver.resolveString(rawVersionStr));
            }
        };
    }

    public String getVersionStr() {
        return versionStr;
    }

    @Override
    public void applyLocation(Applier applier) {
        applier.applyVersion(versionStr);
    }

    @Override
    public String toLocalizedString() {
        return NbStrings.getGradleLocationVersion(versionStr);
    }
}
