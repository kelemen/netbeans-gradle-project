package org.netbeans.gradle.project.query;

import java.util.regex.Pattern;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleSourceLevelQueryImplementation implements SourceLevelQueryImplementation2 {
    private final NbGradleProject project;

    public GradleSourceLevelQueryImplementation(NbGradleProject project) {
        this.project = project;
    }

    @Override
    public Result getSourceLevel(FileObject javaFile) {
        // Assume that every source file must reside in the project directory.
        if (FileUtil.getRelativePath(project.getProjectDirectory(), javaFile) == null) {
            return null;
        }

        String version = JavaPlatform.getDefault().getSpecification().getVersion().toString();
        String[] parts = version.split(Pattern.quote("."), 3);

        final String normalizedVersion = parts.length >= 2
                ? parts[0] + "." + parts[1]
                : null;

        if (normalizedVersion == null) {
            return null;
        }

        return new Result() {
            @Override
            public String getSourceLevel() {
                return normalizedVersion;
            }

            @Override
            public void addChangeListener(ChangeListener listener) {
                // No changes
            }

            @Override
            public void removeChangeListener(ChangeListener listener) {
                // No changes
            }
        };
    }
}
