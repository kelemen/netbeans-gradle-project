/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.netbeans.gradle.project;

import java.io.File;
import java.net.MalformedURLException;
import java.net.URI;
import org.netbeans.api.queries.SharabilityQuery.Sharability;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleSharabilityQuery implements SharabilityQueryImplementation2 {
    private final NbGradleProject project;

    public GradleSharabilityQuery(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public Sharability getSharability(URI uri) {
        FileObject projectDir = project.getProjectDirectory();
        FileObject buildDir = projectDir.getFileObject("build/");
        File uriAsFile;
        try {
            uriAsFile = FileUtil.archiveOrDirForURL(uri.toURL());
        } catch (MalformedURLException ex) {
            uriAsFile = null;
        }

        FileObject queriedFile = uriAsFile != null
                ? FileUtil.toFileObject(uriAsFile)
                : null;

        if (queriedFile == null) {
            return Sharability.UNKNOWN;
        }

        if (buildDir != null && FileUtil.getRelativePath(buildDir, queriedFile) != null) {
            return Sharability.NOT_SHARABLE;
        }

        if (projectDir != null && FileUtil.getRelativePath(projectDir, queriedFile) != null) {
            return Sharability.SHARABLE;
        }

        return Sharability.UNKNOWN;
    }
}
