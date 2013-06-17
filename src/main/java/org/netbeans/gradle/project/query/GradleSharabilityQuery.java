package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import org.netbeans.api.queries.SharabilityQuery.Sharability;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleSharabilityQuery implements SharabilityQueryImplementation2 {
    private final NbGradleProject project;

    public GradleSharabilityQuery(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    private static boolean isInDirectory(FileObject containingDir, String subDir, FileObject queriedFile) {
        FileObject dir = containingDir.getFileObject(subDir);
        if (dir == null) {
            return false;
        }

        return FileUtil.getRelativePath(dir, queriedFile) != null;
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

    @Override
    public Sharability getSharability(URI uri) {
        FileObject queriedFile = uriAsFileObject(uri);

        if (queriedFile == null) {
            return Sharability.UNKNOWN;
        }

        FileObject projectDir = project.getProjectDirectory();
        if (isInDirectory(projectDir, "build/", queriedFile)) {
            return Sharability.NOT_SHARABLE;
        }
        if (isInDirectory(projectDir, ".nb-gradle/private/", queriedFile)) {
            return Sharability.NOT_SHARABLE;
        }

        if (FileUtil.getRelativePath(projectDir, queriedFile) != null) {
            return Sharability.SHARABLE;
        }

        return Sharability.UNKNOWN;
    }
}
