package org.netbeans.gradle.project.query;

import java.nio.charset.Charset;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleSourceEncodingQuery extends FileEncodingQueryImplementation {
    private final NbGradleProject project;

    public GradleSourceEncodingQuery(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public Charset getEncoding(FileObject file) {
        if (FileUtil.isParentOf(project.getProjectDirectory(), file)) {
            ProjectProperties properties = project.tryGetLoadedProperties();
            return properties != null
                    ? properties.getSourceEncoding().getValue()
                    : null;
        }
        else {
            return null;
        }
    }
}
