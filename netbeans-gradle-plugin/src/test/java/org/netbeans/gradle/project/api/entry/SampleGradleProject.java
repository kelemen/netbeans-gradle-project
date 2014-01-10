package org.netbeans.gradle.project.api.entry;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.util.GradleVersion;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.model.util.ZipUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationVersion;
import org.openide.filesystems.FileUtil;

public final class SampleGradleProject implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(SampleGradleProject.class.getName());
    public static final String DEFAULT_GRADLE_VERSION = GradleVersion.current().getVersion();
    public static final GradleLocation DEFAULT_GRADLE_TARGET = new GradleLocationVersion(DEFAULT_GRADLE_VERSION);

    private final File tempFolder;

    public SampleGradleProject(File tempFolder) {
        if (tempFolder == null) throw new NullPointerException("tempFolder");
        this.tempFolder = tempFolder;
    }

    public static SampleGradleProject createProject(String resourceRelPath) throws IOException {
        return createProject(SampleGradleProject.class, resourceRelPath);
    }

    public static SampleGradleProject createProject(
            Class<?> resourceBase,
            String resourceRelPath) throws IOException {

        File tempFolder = null;
        try {
            return new SampleGradleProject(ZipUtils.unzipResourceToTemp(resourceBase, resourceRelPath));
        } catch (Throwable ex) {
            try {
                if (tempFolder != null) {
                    ZipUtils.recursiveDelete(tempFolder);
                }
            } catch (Throwable subEx) {
                LOGGER.log(Level.SEVERE, "Suppressing exception.", subEx);
            }

            if (ex instanceof IOException) {
                throw (IOException)ex;
            }
            throw Exceptions.throwUnchecked(ex);
        }
    }

    private static File subDir(File base, String... subDirs) throws IOException {
        File result = base;
        for (String subDir: subDirs) {
            result = new File(result, subDir);
        }
        return result.getCanonicalFile();
    }

    @SuppressWarnings("UseSpecificCatch")
    public LoadedProject loadProject(String... projectPath) throws IOException {
        File projectDir = subDir(tempFolder, projectPath);
        final Closeable safeToOpenRef = NbGradleProjectFactory.safeToOpen(projectDir);

        try {
            Project project = ProjectManager.getDefault().findProject(FileUtil.toFileObject(projectDir));
            if (project == null) {
                throw new IOException("Project does not exist: " + Arrays.toString(projectPath));
            }

            final NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
            if (gradleProject == null) {
                throw new IOException("Not a Gradle project: " + Arrays.toString(projectPath));
            }

            return new LoadedProject() {
                @Override
                public NbGradleProject getProject() {
                    return gradleProject;
                }

                @Override
                public void close() throws IOException {
                    safeToOpenRef.close();
                }
            };
        } catch (Throwable ex) {
            throw Exceptions.throwUnchecked(ex);
        }
    }

    @Override
    public void close() throws IOException {
        ZipUtils.recursiveDelete(tempFolder);
    }
}
