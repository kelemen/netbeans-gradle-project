package org.netbeans.gradle.project.properties;

import java.io.File;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbStrings;
import org.openide.filesystems.FileUtil;

public final class GradleLocationDirectory implements GradleLocation {
    public static final String UNIQUE_TYPE_NAME = "INST";

    private final File gradleHome;

    public GradleLocationDirectory(File gradleHome) {
        ExceptionHelper.checkNotNullArgument(gradleHome, "gradleHome");
        this.gradleHome = FileUtil.normalizeFile(gradleHome);
    }

    public File getGradleHome() {
        return gradleHome;
    }

    @Override
    public void applyLocation(Applier applier) {
        applier.applyDirectory(gradleHome);
    }

    @Override
    public String asString() {
        return gradleHome.getPath();
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
