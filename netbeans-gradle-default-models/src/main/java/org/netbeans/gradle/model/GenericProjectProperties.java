package org.netbeans.gradle.model;

import java.io.File;
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

    private final String projectName;
    private final String projectFullName;
    private final File projectDir;
    private final File buildScript;

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

        if (projectName == null) throw new NullPointerException("projectName");
        if (projectFullName == null) throw new NullPointerException("projectFullName");
        if (projectDir == null) throw new NullPointerException("projectDir");

        this.projectName = projectName;
        this.projectFullName = projectFullName;
        this.projectDir = projectDir;
        this.buildScript = buildScript;
    }

    /**
     * Returns the short name of the project as returned by
     * {@link org.gradle.api.Project.getName()}.
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
}
