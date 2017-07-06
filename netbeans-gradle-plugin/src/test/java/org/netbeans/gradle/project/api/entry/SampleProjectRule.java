package org.netbeans.gradle.project.api.entry;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Consumer;
import org.jtrim2.utils.ExceptionHelper;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.util.CustomGlobalSettingsRule;
import org.netbeans.gradle.project.util.MockServicesRule;
import org.openide.filesystems.FileObject;

public final class SampleProjectRule implements TestRule {
    private final String resourceName;
    private final TestRule[] wrapperRules;
    private final AtomicReference<SampleGradleProject> projectRef;

    private final ReentrantLock loadedProjectsLock;
    private Map<FileObject, Project> loadedProjects;

    public SampleProjectRule(String resourceName, TestRule... wrapperRules) {
        this.resourceName = Objects.requireNonNull(resourceName, "resourceName");
        this.wrapperRules = wrapperRules.clone();
        this.projectRef = new AtomicReference<>(null);
        this.loadedProjectsLock = new ReentrantLock();
        this.loadedProjects = null;
        ExceptionHelper.checkNotNullElements(this.wrapperRules, "wrapperRules");
    }

    private static Map<FileObject, Project> newProjectContainer() {
        return new HashMap<>();
    }

    public static SampleProjectRule getStandardRule(
            String resourceName,
            Consumer<CommonGlobalSettings> additionalConfig,
            Class<?>... services) {

        Consumer<CommonGlobalSettings> settingsConfigurer = (CommonGlobalSettings settings) -> {
            settings.gradleLocation().setValue(SampleGradleProject.DEFAULT_GRADLE_TARGET);
            settings.gradleJvmArgs().setValue(Arrays.asList("-Xmx256m"));
            settings.defaultJdk().setValue(ScriptPlatform.getDefault());

            additionalConfig.accept(settings);
        };

        return new SampleProjectRule(resourceName,
                new CustomGlobalSettingsRule(settingsConfigurer),
                new MockServicesRule(services));
    }

    public static SampleProjectRule getStandardRule(String resourceName, Class<?>... services) {
        return getStandardRule(resourceName, settings -> { }, services);
    }

    private void introduceProject(Project project) {
        loadedProjectsLock.lock();
        try {
            if (loadedProjects != null) {
                loadedProjects.put(project.getProjectDirectory(), project);
            }
        } finally {
            loadedProjectsLock.unlock();
        }
    }

    public SampleGradleProject getSampleProject() {
        SampleGradleProject project = projectRef.get();
        if (project == null) {
            throw new IllegalStateException("Rule is inactive.");
        }
        return project;
    }

    private static void waitAllLoaded(Collection<? extends Project> projects) throws TimeoutException {
        for (Project project: projects) {
            NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
            if (gradleProject != null) {
                waitLoaded(gradleProject);
            }
        }
    }

    private static void waitLoaded(NbGradleProject project) throws TimeoutException {
        if (!project.tryWaitForLoadedProject(3, TimeUnit.MINUTES)) {
            throw new TimeoutException("Project was not loaded until the timeout elapsed: "
                    + Arrays.asList(project.getProjectDirectoryAsPath()));
        }
    }

    public Project getUnloadedProject(String... projectPath) throws IOException {
        Project result = getSampleProject().getUnloadedProject(projectPath);
        introduceProject(result);
        return result;
    }

    public NbGradleProject loadProject(String... projectPath) throws IOException {
        Thread.interrupted();
        NbGradleProject result = getSampleProject().loadProject(projectPath);
        introduceProject(result);
        return result;
    }

    public NbGradleProject loadAndWaitProject(String... projectPath) throws IOException, TimeoutException {
        NbGradleProject result = loadProject(projectPath);
        waitLoaded(result);
        return result;
    }

    public NbGradleProject loadSingleProject() throws IOException {
        Thread.interrupted();
        NbGradleProject result = getSampleProject().loadSingleProject();
        introduceProject(result);
        return result;
    }

    public NbGradleProject loadAndWaitSingleProject() throws IOException, TimeoutException {
        NbGradleProject result = loadSingleProject();
        waitLoaded(result);
        return result;
    }

    @Override
    public Statement apply(final Statement base, final Description description) {
        Statement result = new Statement() {
            @Override
            public void evaluate() throws Throwable {
                evaluate0(base, description);
            }
        };

        for (int i = wrapperRules.length - 1; i >= 0; i--) {
            result = wrapperRules[i].apply(result, description);
        }

        return result;
    }

    private void evaluate0(Statement base, Description description) throws Throwable {
        SampleGradleProject project = SampleGradleProject.createProject(description.getTestClass(), resourceName);
        try {
            loadedProjectsLock.lock();
            try {
                loadedProjects = newProjectContainer();
            } finally {
                loadedProjectsLock.unlock();
            }

            if (!projectRef.compareAndSet(null, project)) {
                project.close();
                throw new IllegalStateException("Rule was called concurrently.");
            }

            base.evaluate();
        } finally {
            project = projectRef.getAndSet(null);
            if (project != null) {
                try {
                    waitAllLoaded(getAndClearAllLoaded());
                } finally {
                    project.close();
                }
            }
        }
    }

    private Collection<Project> getAndClearAllLoaded() {
        loadedProjectsLock.lock();
        try {
            Map<?, Project> projects = loadedProjects;
            if (projects == null) {
                return Collections.emptySet();
            }
            loadedProjects = null;
            return new ArrayList<>(projects.values());
        } finally {
            loadedProjectsLock.unlock();
        }
    }
}
