package org.netbeans.gradle.project.api.entry;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.util.GradleVersion;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.model.util.ZipUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationVersion;
import org.openide.filesystems.FileUtil;

public final class SampleGradleProject implements Closeable {
    private static final Logger LOGGER = Logger.getLogger(SampleGradleProject.class.getName());
    public static final String DEFAULT_GRADLE_VERSION = GradleVersion.current().getVersion();
    public static final GradleLocation DEFAULT_GRADLE_TARGET = new GradleLocationVersion(DEFAULT_GRADLE_VERSION);

    private static final int DAEMON_TIMEOUT_SEC = 60;

    private final File tempFolder;

    public SampleGradleProject(File tempFolder) {
        ExceptionHelper.checkNotNullArgument(tempFolder, "tempFolder");
        this.tempFolder = tempFolder;
    }

    public static SampleGradleProject createProject(String resourceRelPath) throws IOException {
        GlobalGradleSettings.getGradleDaemonTimeoutSec().setValue(DAEMON_TIMEOUT_SEC);
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

    private Project getUnloadedProject(File projectDir) throws IOException {
        File normProjectDir = FileUtil.normalizeFile(projectDir);
        try (Closeable safeToOpenRef = NbGradleProjectFactory.safeToOpen(normProjectDir)) {
            assert safeToOpenRef != null; // Avoid warning

            Project project = ProjectManager.getDefault().findProject(FileUtil.toFileObject(normProjectDir));
            if (project == null) {
                throw new IllegalArgumentException("Project does not exist: " + normProjectDir);
            }

            return project;
        } catch (IOException ex) {
            throw Exceptions.throwUnchecked(ex);
        }
    }

    public Project getUnloadedProject(String... projectPath) throws IOException {
        File projectDir = subDir(tempFolder, projectPath);
        return getUnloadedProject(projectDir);
    }

    private NbGradleProject toLoadedProject(Project project) throws IOException {
        try {
            final NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
            if (gradleProject == null) {
                throw new IllegalArgumentException("Not a Gradle project: " + project.getProjectDirectory());
            }

            gradleProject.ensureLoadRequested();

            return gradleProject;
        } catch (Throwable ex) {
            throw Exceptions.throwUnchecked(ex);
        }
    }

    public Project getSingleUnloadedProject() throws IOException {
        File[] dirs = tempFolder.listFiles();
        if (dirs.length != 1) {
            throw new IllegalStateException(tempFolder + " does not contain a single folder but " + dirs.length);
        }

        return getUnloadedProject(dirs[0]);
    }

    public NbGradleProject loadSingleProject() throws IOException {
        Project project = getSingleUnloadedProject();
        return toLoadedProject(project);
    }

    public NbGradleProject loadProject(String... projectPath) throws IOException {
        Project project = getUnloadedProject(projectPath);
        return toLoadedProject(project);
    }

    @Override
    public void close() throws IOException {
        ZipUtils.recursiveDelete(tempFolder);
    }
}
