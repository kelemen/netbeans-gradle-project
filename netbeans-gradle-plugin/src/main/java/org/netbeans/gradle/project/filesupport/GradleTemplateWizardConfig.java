package org.netbeans.gradle.project.filesupport;

import java.nio.file.Path;
import org.jtrim.utils.ExceptionHelper;

final class GradleTemplateWizardConfig {
    private final Path gradleFile;

    public GradleTemplateWizardConfig(Path gradleFile) {
        ExceptionHelper.checkNotNullArgument(gradleFile, "gradleFile");
        this.gradleFile = gradleFile;
    }

    public Path getGradleFile() {
        return gradleFile;
    }
}
