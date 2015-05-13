package org.netbeans.gradle.project.query;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.queries.SharabilityQuery.Sharability;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;

public final class GradleSharabilityQuery implements SharabilityQueryImplementation2 {
    private final NbGradleProject project;

    public GradleSharabilityQuery(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
    }

    private static boolean isInDirectory(Path dir, Path queriedFile) {
        return queriedFile.startsWith(dir);
    }

    private static boolean isInBuildDir(NbGradleModel model, Path queriedFile) {
        Path buildDir = model.getGenericInfo().getBuildDir().toPath();
        return isInDirectory(buildDir, queriedFile);
    }

    private static Path tryConvertToPath(URI uri) {
        try {
            return Paths.get(uri);
        } catch (IllegalArgumentException | FileSystemNotFoundException e) {
            return null;
        }
    }

    @Override
    public Sharability getSharability(URI uri) {
        Path queriedPath = tryConvertToPath(uri);
        if (queriedPath == null) {
            return Sharability.UNKNOWN;
        }

        NbGradleModel model = project.currentModel().getValue();
        Path rootProjectDir = model.getSettingsDir();

        if (isInBuildDir(model, queriedPath)) {
            return Sharability.NOT_SHARABLE;
        }
        if (isInDirectory(SettingsFiles.getSettingsDir(rootProjectDir), queriedPath)) {
            return Sharability.NOT_SHARABLE;
        }
        if (isInDirectory(SettingsFiles.getPrivateSettingsDir(rootProjectDir), queriedPath)) {
            return Sharability.NOT_SHARABLE;
        }

        Path projectDir = model.getProjectDir().toPath();
        if (isInDirectory(projectDir, queriedPath)) {
            return Sharability.SHARABLE;
        }

        return Sharability.UNKNOWN;
    }
}
