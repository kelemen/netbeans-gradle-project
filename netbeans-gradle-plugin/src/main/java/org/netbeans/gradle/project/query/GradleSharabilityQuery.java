package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.queries.SharabilityQuery.Sharability;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleSharabilityQuery implements SharabilityQueryImplementation2 {
    private final NbGradleProject project;

    public GradleSharabilityQuery(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
    }

    private static boolean isInDirectory(FileObject dir, FileObject queriedFile) {
        return FileUtil.getRelativePath(dir, queriedFile) != null;
    }

    private static boolean isInDirectory(FileObject containingDir, String subDir, FileObject queriedFile) {
        FileObject dir = containingDir.getFileObject(subDir);
        if (dir == null) {
            return false;
        }

        return isInDirectory(dir, queriedFile);
    }

    private static FileObject uriAsFileObject(URI uri) {
        File uriAsFile;
        try {
            uriAsFile = FileUtil.archiveOrDirForURL(uri.toURL());
        } catch (MalformedURLException ex) {
            return null;
        }

        return uriAsFile != null
                ? FileUtil.toFileObject(uriAsFile)
                : null;
    }

    private static boolean isInBuildDir(NbGradleModel model, FileObject queriedFile) {
        File buildDir = model.getGenericInfo().getBuildDir();
        FileObject buildDirObj = FileUtil.toFileObject(buildDir);
        if (buildDirObj == null) {
            return false;
        }

        return isInDirectory(buildDirObj, queriedFile);
    }

    @Override
    public Sharability getSharability(URI uri) {
        FileObject queriedFile = uriAsFileObject(uri);

        if (queriedFile == null) {
            return Sharability.UNKNOWN;
        }

        FileObject projectDir = project.getProjectDirectory();
        NbGradleModel model = project.currentModel().getValue();

        if (isInBuildDir(model, queriedFile)) {
            return Sharability.NOT_SHARABLE;
        }
        if (isInDirectory(projectDir, ".nb-gradle/private/", queriedFile)) {
            return Sharability.NOT_SHARABLE;
        }
        if (isInDirectory(projectDir, ".nb-gradle/profiles/private/", queriedFile)) {
            return Sharability.NOT_SHARABLE;
        }

        if (FileUtil.getRelativePath(projectDir, queriedFile) != null) {
            return Sharability.SHARABLE;
        }

        return Sharability.UNKNOWN;
    }
}
