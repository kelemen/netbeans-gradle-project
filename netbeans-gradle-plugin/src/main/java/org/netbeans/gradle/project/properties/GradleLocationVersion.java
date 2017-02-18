package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.tasks.vars.StandardTaskVariable;

public final class GradleLocationVersion implements GradleLocation {
    public static final String UNIQUE_TYPE_NAME = "VER";

    private final String baseVersionStr;
    private final String versionStr;

    public GradleLocationVersion(String versionStr) {
        ExceptionHelper.checkNotNullArgument(versionStr, "versionStr");
        this.baseVersionStr = versionStr;
        this.versionStr = StandardTaskVariable.replaceGlobalVars(versionStr);
    }

    public String getVersionStr() {
        return versionStr;
    }

    @Override
    public void applyLocation(Applier applier) {
        applier.applyVersion(versionStr);
    }

    @Override
    public String asString() {
        return baseVersionStr;
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
