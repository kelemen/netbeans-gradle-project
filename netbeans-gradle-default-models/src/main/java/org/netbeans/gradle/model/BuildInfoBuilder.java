package org.netbeans.gradle.model;

import java.io.Serializable;
import org.gradle.tooling.BuildController;

/**
 * Defines a method which extracts the required information from the Gradle
 * build.
 * <P>
 * The serialized format of this class does not require any kind of backward
 * or forward compatibility.
 *
 * @param <T> the type of the object the {@code BuildInfoBuilder} extracts
 *   from Gradle build
 *
 * @see GradleBuildInfoQuery
 */
public interface BuildInfoBuilder<T> extends Serializable {
    /**
     * Extracts some information from the Gradle build using the given
     * {@code BuildController}.
     * <P>
     * This methods should never throw an exception. Throwing an exception is
     * considered a serious failure. Therefore this method must handle the case
     * when the information cannot be accessed (e.g.: due to a model not being
     * available).
     *
     * @param controller the {@code BuildController} which is to be used to
     *   extract the required information. This argument cannot be {@code null}.
     *
     * @return the required information extracted from the Gradle build. This
     *   method may return {@code null}. which is interpreted as no useful
     *   information could be extracted.
     */
    public T getInfo(BuildController controller);

    /**
     * Returns a string used to identify this {@code BuildInfoBuilder}
     * instance if this builder fails with an unexpected exception. If the
     * returned string has a format or not is an implementation detail but
     * usually it is enough to give builders a name which allows easy
     * identification.
     *
     * @return a string used to identify this {@code BuildInfoBuilder}
     *   instance. This method may never return {@code null}.
     */
    public String getName();
}
