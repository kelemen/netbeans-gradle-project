package org.netbeans.gradle.project.api.nodes;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * {@code GradleProjectExtensionNodes} implementations should be annotated with
 * this annotation if they call their
 * {@link GradleProjectExtensionNodes#addNodeChangeListener(Runnable) node change listeners}
 * properly. Extensions are highly recommended to call their node change listeners
 * properly (and use this annotation) otherwise every project load will cause
 * the project nodes to collapse.
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface ManualRefreshedNodes {
}
