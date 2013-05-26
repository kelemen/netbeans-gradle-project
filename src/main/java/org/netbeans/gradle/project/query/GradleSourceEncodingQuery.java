package org.netbeans.gradle.project.query;

import java.nio.charset.Charset;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.ProjectProperties;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleSourceEncodingQuery extends FileEncodingQueryImplementation {
    private static final Logger LOGGER = Logger.getLogger(GradleSourceEncodingQuery.class.getName());

    private final NbGradleProject project;

    public GradleSourceEncodingQuery(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
    }

    @Override
    public Charset getEncoding(FileObject file) {
        if (FileUtil.isParentOf(project.getProjectDirectory(), file)) {
            try {
                ProjectProperties properties = project.tryGetLoadedProperties();
                if (properties != null) {
                    return properties.getSourceEncoding().getValue();
                }
            } catch (IllegalStateException ex) {
                LOGGER.log(Level.INFO, "The character encoding of a file has been requested from a wrong thread.", ex);
            }

            // We don't expect that anyone else can tell what the encoding of
            // the file is, so return what we have (probably "UTF-8") is more
            // reliable than the platform's default encoding.
            return project.getProperties().getSourceEncoding().getValue();
        }
        else {
            return null;
        }
    }
}
