package org.netbeans.gradle.model;

import java.io.File;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Defines properties common to all kinds of projects in Gradle.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 */
public final class GenericProjectProperties implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ProjectId projectId;
    private final String projectName;
    private final String projectFullName;
    private final File projectDir;
    private final File buildScript;
    private final File buildDir;

    /**
     * Creates a new {@code GenericProjectProperties} with the specified
     * properties.
     *
     * @param projectName the short name of the project as returned by
     *   {@link org.gradle.api.Project.getName()}. This argument cannot be
     *   {@code null}.
     * @param projectFullName the fully-qualified name of the project as
     *   returned by {@link org.gradle.api.Project.getPath()}. This argument
     *   cannot be {@code null}.
     * @param projectDir the project directory as returned by
     *   {@link org.gradle.api.Project.getProjectDir()}. This argument cannot
     *   be {@code null}.
     * @param buildScript the path to the build script file. Can be {@code null},
     *   if there is no build script file to use.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public GenericProjectProperties(
            String projectName,
            String projectFullName,
            File projectDir,
            File buildScript) {
        this(defaultProjectId(projectName), projectFullName, projectDir, buildScript);
    }

    /**
     * Creates a new {@code GenericProjectProperties} with the specified
     * properties.
     *
     * @param projectId the group, name and version properties of the
     *   {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null}.
     * @param projectFullName the fully-qualified name of the project as
     *   returned by {@link org.gradle.api.Project.getPath()}. This argument
     *   cannot be {@code null}.
     * @param projectDir the project directory as returned by
     *   {@link org.gradle.api.Project.getProjectDir()}. This argument cannot
     *   be {@code null}.
     * @param buildScript the path to the build script file. Can be {@code null},
     *   if there is no build script file to use.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public GenericProjectProperties(
            ProjectId projectId,
            String projectFullName,
            File projectDir,
            File buildScript) {
        this(projectId, projectFullName, projectDir, buildScript, defaultBuildDir(projectDir));
    }

    /**
     * Creates a new {@code GenericProjectProperties} with the specified
     * properties.
     *
     * @param projectId the group, name and version properties of the
     *   {@link org.gradle.api.Project} instance. This argument cannot be
     *   {@code null}.
     * @param projectFullName the fully-qualified name of the project as
     *   returned by {@link org.gradle.api.Project.getPath()}. This argument
     *   cannot be {@code null}.
     * @param projectDir the project directory as returned by
     *   {@link org.gradle.api.Project.getProjectDir()}. This argument cannot
     *   be {@code null}.
     * @param buildScript the path to the build script file. Can be {@code null},
     *   if there is no build script file to use.
     * @param buildDir the directory where build results should be put by
     *   tasks, that is the directory returned by
     *   {@link org.gradle.api.Project.getBuildDir()}. This argument
     *   cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public GenericProjectProperties(
            ProjectId projectId,
            String projectFullName,
            File projectDir,
            File buildScript,
            File buildDir) {

        if (projectId == null) throw new NullPointerException("projectId");
        if (projectFullName == null) throw new NullPointerException("projectFullName");
        if (projectDir == null) throw new NullPointerException("projectDir");
        if (buildDir == null) throw new NullPointerException("buildDir");

        this.projectId = projectId;
        this.projectName = projectId.getName();
        this.projectFullName = projectFullName;
        this.projectDir = projectDir;
        this.buildScript = buildScript;
        this.buildDir = buildDir;
    }

    private static ProjectId defaultProjectId(String name) {
        return new ProjectId("?", name, "?");
    }

    private static File defaultBuildDir(File projecDir) {
        return new File(projecDir, "build");
    }

    /**
     * Returns the group, name and version property of the {@link org.gradle.api.Project}
     * instance.
     *
     * @return the group, name and version property of the {@link org.gradle.api.Project}
     *   instance. This method never returns {@code null}.
     */
    public ProjectId getProjectId() {
        return projectId;
    }

    /**
     * Returns the short name of the project as returned by
     * {@link org.gradle.api.Project.getName()}. This method is equivalent to
     * calling {@code getProjectId().getName()}.
     *
     * @return the short name of the project. This method never returns
     *   {@code null}.
     */
    public String getProjectName() {
        return projectName;
    }

    /**
     * Returns the fully-qualified name of the project as returned by
     * {@link org.gradle.api.Project.getPath()}.
     *
     * @return the fully qualified name of the project. This method never
     *   returns {@code null}.
     */
    public String getProjectFullName() {
        return projectFullName;
    }

    /**
     * Returns the project directory as returned by
     * {@link org.gradle.api.Project.getProjectDir()}.
     *
     * @return the project directory. This method never returns {@code null}.
     */
    public File getProjectDir() {
        return projectDir;
    }

    /**
     * The build script file of this project. This is the "build.gradle" file
     * in the directory of the project by default.
     *
     * @return the build script file of this project. This method may return
     *   {@code null} if there is no build script for this project.
     */
    public File getBuildScript() {
        return buildScript;
    }

    /**
     * Returns the directory where Gradle tasks should put their output to.
     * That is, the directory returned by {@link org.gradle.api.Project.getBuildDir()}.
     *
     * @return the directory where Gradle tasks should put their output to.
     *   This method never returns {@code null}.
     */
    public File getBuildDir() {
        return buildDir;
    }

    private Object readResolve() throws ObjectStreamException {
        // The null check is there for backward compatibility.
        // That is, when this object was serialized with a previous version
        // of this class.
        if (projectId  != null && buildDir != null) {
            return this;
        }

        return new GenericProjectProperties(
                defaultProjectId(projectName),
                projectFullName,
                projectDir,
                buildScript,
                defaultBuildDir(projectDir));
    }
}
