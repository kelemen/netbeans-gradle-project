package org.netbeans.gradle.project.api.nodes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Action implementations returned by {@link GradleProjectContextActions#getContextActions()},
 * might be annotated with this annotation to give a hint to the Gradle plugin
 * where the action should be displayed in the project's context menu.
 *
 * @see GradleActionType
 * @see GradleProjectContextActions
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface GradleProjectAction {
    GradleActionType value();
}
