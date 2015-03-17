package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;

public final class GradleLocationVersion implements GradleLocation {
    public static final String UNIQUE_TYPE_NAME = "VER";

    private final String versionStr;

    public GradleLocationVersion(String versionStr) {
        ExceptionHelper.checkNotNullArgument(versionStr, "versionStr");
        this.versionStr = versionStr;
    }

    public String getVersionStr() {
        return versionStr;
    }

    @Override
    public void applyLocation(NbGradleProject project, Applier applier) {
        applier.applyVersion(versionStr);
    }

    @Override
    public String asString() {
        return versionStr;
    }

    @Override
    public String getUniqueTypeName() {
        return UNIQUE_TYPE_NAME;
    }

    @Override
    public String toLocalizedString() {
        return NbStrings.getGradleLocationVersion(versionStr);
    }
}
