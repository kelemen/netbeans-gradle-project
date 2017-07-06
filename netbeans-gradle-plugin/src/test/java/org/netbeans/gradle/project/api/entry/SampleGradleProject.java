package org.netbeans.gradle.project.api.entry;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import org.gradle.util.GradleVersion;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.model.util.ZipUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;

public final class SampleGradleProject implements Closeable {
    public static final String DEFAULT_GRADLE_VERSION = GradleVersion.current().getVersion();
    public static final GradleLocationDef DEFAULT_GRADLE_TARGET = GradleLocationDef.fromVersion(DEFAULT_GRADLE_VERSION, false);

    private static final int DAEMON_TIMEOUT_SEC = 60;

    private final File tempFolder;

    public SampleGradleProject(File tempFolder) {
        this.tempFolder = Objects.requireNonNull(tempFolder, "tempFolder");
    }

    public static SampleGradleProject createProject(String resourceRelPath) throws IOException {
        return createProject(SampleGradleProject.class, resourceRelPath);
    }

    public static SampleGradleProject createProject(
            Class<?> resourceBase,
            String resourceRelPath) throws IOException {
        CommonGlobalSettings.getDefault().gradleDaemonTimeoutSec().setValue(DAEMON_TIMEOUT_SEC);
        return new SampleGradleProject(ZipUtils.unzipResourceToTemp(resourceBase, resourceRelPath));
    }

    private static File subDir(File base, String... subDirs) throws IOException {
        File result = base;
        for (String subDir: subDirs) {
            result = new File(result, subDir);
        }
        return result.getCanonicalFile();
    }

    private Project getUnloadedProject(File projectDir) throws IOException {
        return NbGradleProjectFactory.loadSafeProject(projectDir);
    }

    public Project getUnloadedProject(String... projectPath) throws IOException {
        File projectDir = subDir(tempFolder, projectPath);
        return getUnloadedProject(projectDir);
    }

    private NbGradleProject toLoadedProject(Project project) throws IOException {
        try {
            NbGradleProject gradleProject = NbGradleProjectFactory.getGradleProject(project);
            gradleProject.ensureLoadRequested();
            return gradleProject;
        } catch (Throwable ex) {
            throw Exceptions.throwUnchecked(ex);
        }
    }

    public Project getSingleUnloadedProject() throws IOException {
        File[] dirs = tempFolder.listFiles();
        int dirsLength = dirs != null ? dirs.length : 0;
        if (dirsLength != 1) {
            throw new IllegalStateException(tempFolder + " does not contain a single folder but " + dirsLength);
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
        tryDelete(tempFolder);
    }

    private static void tryDelete(File folder) throws IOException {
        long startTime = System.nanoTime();
        long maxWaitNanos = TimeUnit.SECONDS.toNanos(60);

        while (true) {
            try {
                ZipUtils.recursiveDelete(folder);
                return;
            } catch (IOException ex) {
                long elapsed = System.nanoTime() - startTime;
                if (elapsed >= maxWaitNanos) {
                    throw ex;
                }
            }
        }
    }
}
