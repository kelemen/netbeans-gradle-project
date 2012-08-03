package org.netbeans.gradle.project;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Pattern;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.spi.java.queries.SourceLevelQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.ChangeSupport;

public final class GradleSourceLevelQueryImplementation implements SourceLevelQueryImplementation2 {
    private final NbGradleProject project;

    public GradleSourceLevelQueryImplementation(NbGradleProject project) {
        this.project = project;
    }

    private static String getSourceLevelFromModel(NbProjectModel projectModel) {
        // FIXME: This is incorrect, because it returns strings like "JDK_1_6".
        return projectModel.getIdeaModel().getLanguageLevel().getLevel();
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

    // This implementation is incorrect for two reasons:
    //
    // 1. It checks the source level from the Idea project which is based on the
    //    "idea" plugin rather than the "java".
    // 2. It does not parse the source level string properly.
    private static class SourceLevelResult implements Result {
        private final NbGradleProject project;
        private volatile NbProjectModel lastProjectModel;
        private final ChangeSupport changes;
        private final AtomicBoolean querying;

        public SourceLevelResult(NbGradleProject project) {
            this.project = project;
            this.lastProjectModel = null;
            this.changes = new ChangeSupport(this);
            this.querying = new AtomicBoolean(false);
        }

        @Override
        public String getSourceLevel() {
            NbProjectModel projectModel = project.tryGetCachedProject();
            if (projectModel != null) {
                return getSourceLevelFromModel(projectModel);
            }
            else {
                if (!querying.getAndSet(true)) {
                    NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                        @Override
                        public void run() {
                            querying.set(false);
                            lastProjectModel = project.loadProject();
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    changes.fireChange();
                                }
                            });
                        }
                    });
                }

                projectModel = lastProjectModel;
                return projectModel != null
                        ? getSourceLevelFromModel(projectModel)
                        : null;
            }
        }

        // TODO: listen for changes in the project.

        @Override
        public void addChangeListener(ChangeListener listener) {
            changes.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            changes.removeChangeListener(listener);
        }
    }
}
