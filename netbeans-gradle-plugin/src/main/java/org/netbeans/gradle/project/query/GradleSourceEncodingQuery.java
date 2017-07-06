package org.netbeans.gradle.project.query;

import java.nio.charset.Charset;
import java.util.Objects;
import org.jtrim2.property.PropertySource;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;

public final class GradleSourceEncodingQuery extends FileEncodingQueryImplementation {
    private final FileObject projectDir;
    private final PropertySource<Charset> sourceEncoding;

    public GradleSourceEncodingQuery(
            FileObject projectDir,
            PropertySource<Charset> sourceEncoding) {
        this.projectDir = Objects.requireNonNull(projectDir, "projectDir");
        this.sourceEncoding = Objects.requireNonNull(sourceEncoding, "sourceEncoding");
    }

    @Override
    public Charset getEncoding(FileObject file) {
        Project ownerProject = FileOwnerQuery.getOwner(file);
        if (ownerProject != null && projectDir.equals(ownerProject.getProjectDirectory())) {
            // We don't expect that anyone else can tell what the encoding of
            // the file is, so return what we have (probably "UTF-8") is more
            // reliable than the platform's default encoding.
            return sourceEncoding.getValue();
        }
        else {
            return null;
        }
    }
}
