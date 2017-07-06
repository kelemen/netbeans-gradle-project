package org.netbeans.gradle.project.properties;

import java.io.File;
import java.util.Objects;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.properties.GradleLocation.Applier;
import org.netbeans.gradle.project.tasks.vars.StringResolver;
import org.openide.filesystems.FileUtil;

public final class GradleLocationDirectory implements GradleLocation {
    public static final String UNIQUE_TYPE_NAME = "INST";

    private final File gradleHome;

    public GradleLocationDirectory(File gradleHome) {
        this.gradleHome = Objects.requireNonNull(gradleHome, "gradleHome");
    }

    public static GradleLocationRef getLocationRef(final String rawDir) {
        Objects.requireNonNull(rawDir, "rawDir");

        return new GradleLocationRef() {
            @Override
            public String getUniqueTypeName() {
                return UNIQUE_TYPE_NAME;
            }

            @Override
            public String asString() {
                return rawDir;
            }

            @Override
            public GradleLocation getLocation(StringResolver resolver) {
                String resolvedDir = resolver.resolveStringIfValid(rawDir);
                if (resolvedDir == null) {
                    return GradleLocationDefault.DEFAULT;
                }

                File gradleHome = FileUtil.normalizeFile(new File(resolvedDir));
                return new GradleLocationDirectory(gradleHome);
            }
        };
    }

    public File tryGetGradleHome() {
        return gradleHome;
    }

    @Override
    public void applyLocation(Applier applier) {
        applier.applyDirectory(gradleHome);
    }

    @Override
    public String toLocalizedString() {
        return NbStrings.getGradleLocationLocal(gradleHome);
    }
}
