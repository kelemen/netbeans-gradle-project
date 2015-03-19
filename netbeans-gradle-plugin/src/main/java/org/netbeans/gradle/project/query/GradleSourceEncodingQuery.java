package org.netbeans.gradle.project.query;

import java.nio.charset.Charset;
import org.jtrim.cancel.Cancellation;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.openide.filesystems.FileObject;

public final class GradleSourceEncodingQuery extends FileEncodingQueryImplementation {
    private final NbGradleProject project;

    public GradleSourceEncodingQuery(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        this.project = project;
    }

    @Override
    public Charset getEncoding(FileObject file) {
        Project ownerProject = FileOwnerQuery.getOwner(file);
        if (ownerProject != null && project.isSameProject(ownerProject)) {
            // We don't expect that anyone else can tell what the encoding of
            // the file is, so return what we have (probably "UTF-8") is more
            // reliable than the platform's default encoding.
            project.getActiveSettingsQuery().waitForLoadedOnce(Cancellation.UNCANCELABLE_TOKEN);
            return project.getCommonProperties().sourceEncoding().getActiveValue();
        }
        else {
            return null;
        }
    }
}
