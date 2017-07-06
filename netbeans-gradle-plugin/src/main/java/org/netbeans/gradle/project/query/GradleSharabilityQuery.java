package org.netbeans.gradle.project.query;

import java.net.URI;
import java.nio.file.FileSystemNotFoundException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Objects;
import org.jtrim2.property.PropertySource;
import org.netbeans.api.queries.SharabilityQuery.Sharability;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;

public final class GradleSharabilityQuery implements SharabilityQueryImplementation2 {
    private final PropertySource<? extends NbGradleModel> modelRef;

    public GradleSharabilityQuery(PropertySource<? extends NbGradleModel> modelRef) {
        this.modelRef = Objects.requireNonNull(modelRef, "modelRef");
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

        NbGradleModel model = modelRef.getValue();
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
