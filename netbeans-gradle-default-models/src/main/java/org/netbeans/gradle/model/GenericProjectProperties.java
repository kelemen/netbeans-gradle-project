package org.netbeans.gradle.model;

import java.io.File;
import java.io.Serializable;

/**
 * Defines properties common to all kinds of projects in Gradle.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 */
public final class GenericProjectProperties implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String projectName;
    private final String projectFullName;
    private final File projectDir;

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
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public GenericProjectProperties(String projectName, String projectFullName, File projectDir) {
        if (projectName == null) throw new NullPointerException("projectName");
        if (projectFullName == null) throw new NullPointerException("projectFullName");
        if (projectDir == null) throw new NullPointerException("projectDir");

        this.projectName = projectName;
        this.projectFullName = projectFullName;
        this.projectDir = projectDir;
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
}
