package org.netbeans.gradle.project.api.nodes;

/**
 * Defines the kinds of actions in the project's context menu. This {@code enum}
 * is to be used with the {@link GradleProjectAction} annotation to mark in
 * which group should your action item be placed.
 *
 * @see GradleProjectAction
 * @see GradleProjectContextActions
 */
public enum GradleActionType {
    /**
     * Defines some kind of build action. Examples are: clean, build,
     * Generate Javadoc.
     * <P>
     * This is the default kind of action, so in this case there is no need
     * to annotate your action implementation.
     */
    BUILD_ACTION,

    /**
     * Defines actions for managing the folders, the layout or something related
     * to managing the project environment. These kinds of project actions
     * belong to the same group as "Reload Project" and "Close".
     */
    PROJECT_MANAGEMENT_ACTION
}
