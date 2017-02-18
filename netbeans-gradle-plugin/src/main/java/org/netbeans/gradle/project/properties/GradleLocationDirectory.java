package org.netbeans.gradle.project.properties;

import java.io.File;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.openide.filesystems.FileUtil;

public final class GradleLocationDirectory implements GradleLocation {
    public static final String UNIQUE_TYPE_NAME = "INST";

    private final String rawGradleHome;
    private final File gradleHome;

    public GradleLocationDirectory(String gradleHome) {
        ExceptionHelper.checkNotNullArgument(gradleHome, "gradleHome");
        this.rawGradleHome = gradleHome;
        this.gradleHome = FileUtil.normalizeFile(new File(StringResolvers.getDefaultGlobalResolver().resolveString(gradleHome)));
    }

    public File tryGetGradleHome() {
        return gradleHome;
    }

    @Override
    public void applyLocation(Applier applier) {
        if (gradleHome != null) {
            applier.applyDirectory(gradleHome);
        }
    }

    @Override
    public String asString() {
        return rawGradleHome;
    }

    @Override
    public String getUniqueTypeName() {
        return UNIQUE_TYPE_NAME;
    }

    @Override
    public String toLocalizedString() {
        return NbStrings.getGradleLocationLocal(gradleHome);
    }
}
