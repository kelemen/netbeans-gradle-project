package org.netbeans.gradle.project.api.modelquery;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Objects;
import javax.annotation.Nonnull;
import org.gradle.util.GradleVersion;
import org.openide.modules.SpecificationVersion;

/**
 * Defines the properties of the target Gradle daemon.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * by multiple threads concurrently.
 */
public final class GradleTarget implements Serializable {
    private static final long serialVersionUID = 1L;

    private final SpecificationVersion jdkVersion;
    private final GradleVersion gradleVersion;

    /**
     * Creates a new {@code GradleTarget} with the given JRE and Gradle version.
     *
     * @param javaVersion the version of JRE used to execute the Gradle daemon.
     *   This argument cannot be {@code null}.
     * @param gradleVersion the version of Gradle used in the Gradle daemon.
     *   This argument cannot be {@code null}.
     */
    public GradleTarget(
            @Nonnull SpecificationVersion javaVersion,
            @Nonnull GradleVersion gradleVersion) {
        this.jdkVersion = Objects.requireNonNull(javaVersion, "javaVersion");
        this.gradleVersion = Objects.requireNonNull(gradleVersion, "gradleVersion");
    }

    /**
     * Returns the version of JRE (or JDK) used to execute the Gradle daemon.
     *
     * @return the version of Java used to execute the Gradle daemon. This
     *   method never returns {@code null}.
     */
    @Nonnull
    public SpecificationVersion getJavaVersion() {
        return jdkVersion;
    }

    /**
     * Returns the version of Gradle used in the Gradle daemon.
     *
     * @return the version of Gradle used in the Gradle daemon.
     */
    @Nonnull
    public GradleVersion getGradleVersion() {
        return gradleVersion;
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String javaVersion;
        private final String gradleVersion;

        public SerializedFormat(GradleTarget source) {
            this.javaVersion = source.getJavaVersion().toString();

            GradleVersion sourceVersion = source.getGradleVersion();
            this.gradleVersion = sourceVersion.getVersion();
        }

        private GradleVersion getGradleVersion() {
            GradleVersion result = GradleVersion.version(gradleVersion);
            if (GradleVersion.current().equals(result)) {
                // There can be properties only set for GradleVersion.current(),
                // so try to be as good as possible (this is only a best effort
                // because the current version might have changed since the version
                // was serialized.
                result = GradleVersion.current();
            }
            return result;
        }

        public SpecificationVersion getJavaVersion() {
            return new SpecificationVersion(javaVersion);
        }

        private Object readResolve() throws ObjectStreamException {
            return new GradleTarget(getJavaVersion(), getGradleVersion());
        }
    }
}
