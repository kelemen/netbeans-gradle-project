package org.netbeans.gradle.model.api;

import java.io.Serializable;
import org.gradle.api.Project;

/**
 * Defines a method which extracts the required information from the Gradle
 * project object.
 * <P>
 * <B>Warning</B>: As of currently, implementations of this interface must be
 * within the "netbeans-gradle-default-models" project.
 * <P>
 * The serialized format of this class does not require any kind of backward
 * or forward compatibility.
 *
 * @param <T> the type of the object the {@code ProjectInfoBuilder} extracts
 *   from the project object
 */
public interface ProjectInfoBuilder<T> extends Serializable {
    /**
     * Extracts some information from the given Gradle project object.
     * <P>
     * This methods should never throw an exception. Throwing an exception is
     * considered a serious failure. Therefore this method must handle the case
     * when the information cannot be accessed (e.g.: due to a plugin not applied).
     *
     * @param project the project object from which the required information is
     *   to be extracted. This argument cannot be {@code null}.
     *
     * @return the required information extracted from the given project. This
     *   method may return {@code null} which is interpreted as no useful
     *   information could be extracted.
     */
    public T getProjectInfo(Project project);
}
