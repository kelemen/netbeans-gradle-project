package org.netbeans.gradle.project.filesupport;

import java.nio.file.Path;
import java.util.Objects;

final class GradleTemplateWizardConfig {
    private final Path gradleFile;

    public GradleTemplateWizardConfig(Path gradleFile) {
        this.gradleFile = Objects.requireNonNull(gradleFile, "gradleFile");
    }

    public Path getGradleFile() {
        return gradleFile;
    }
}
