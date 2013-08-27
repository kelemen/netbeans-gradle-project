package org.netbeans.gradle.model.java;

import java.io.Serializable;

/**
 * Defines the version of Java required to compile and run the associated
 * project.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 */
public final class JavaCompatibilityModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String sourceCompatibility;
    private final String targetCompatibility;

    /**
     * Creates a new {@code JavaCompatibilityModel} with the given properties.
     *
     * @param sourceCompatibility the Java to use to determine the allowed
     *   syntax when compiling the sources. This argument cannot be {@code null}.
     * @param targetCompatibility the Java version to use to generate class for.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is {@code null}
     */
    public JavaCompatibilityModel(String sourceCompatibility, String targetCompatibility) {
        if (sourceCompatibility == null) throw new NullPointerException("sourceCompatibility");
        if (targetCompatibility == null) throw new NullPointerException("targetCompatibility");

        this.sourceCompatibility = sourceCompatibility;
        this.targetCompatibility = targetCompatibility;
    }

    /**
     * Returns the Java to use to determine the allowed syntax when compiling
     * the sources. For example, diamond syntax is only allowed from version
     * "1.7". This property is determined by the "sourceCompatibility" of the
     * <A href="http://www.gradle.org/docs/current/userguide/java_plugin.html">Java plugin of Gradle</A>.
     * <P>
     * This method returns the version in the form: "1.X". That is, the same
     * way as {@code org.gradle.api.JavaVersion.toString()} does.
     *
     * @return the Java to use to determine the allowed syntax when compiling
     *   the sources. This method never returns {@code null}.
     */
    public String getSourceCompatibility() {
        return sourceCompatibility;
    }

    /**
     * Returns the Java version to use to generate class for. That is, JVM with
     * a version of at least the returned value should be able to load the
     * class (although, if you have been using classes from a newer JDK, they
     * won't be found). This property is determined by the "targetCompatibility" of the
     * <A href="http://www.gradle.org/docs/current/userguide/java_plugin.html">Java plugin of Gradle</A>.
     * <P>
     * This method returns the version in the form: "1.X". That is, the same
     * way as {@code org.gradle.api.JavaVersion.toString()} does.
     *
     * @return the Java version to use to generate class for. This method never
     *   returns {@code null}.
     */
    public String getTargetCompatibility() {
        return targetCompatibility;
    }
}
