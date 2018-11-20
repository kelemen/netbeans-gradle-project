package org.netbeans.gradle.model;

import java.io.Serializable;
import org.gradle.api.JavaVersion;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.model.build.BuildEnvironment;
import org.netbeans.gradle.model.util.BuilderUtils;

/**
 * Defines the Gradle version and the JVM evaluating the build scripts.
 * <P>
 * Instances of this class are immutable and therefore are safe to be shared
 * across multiple threads.
 * <P>
 * The serialized format of this class is not subject to any kind of backward
 * or forward compatibility.
 *
 * @see #platformBuilder()
 * @see GradleBuildInfoQuery
 */
public final class GradlePlatformInfo implements Serializable {
    private static final long serialVersionUID = 1L;

    // FIXME: Requires custom serialization to handle the case where a
    // JavaVersion is returned of which the calling Tooling API does not know
    // (i.e.: the case where the calling Tooling API is older than the Gradle
    // being called).

    private final BuildEnvironment buildEnvironment;
    private final JavaVersion javaVersion;

    /**
     * Creates a new {@code GradlePlatformInfo} with the given properties.
     *
     * @param buildEnvironment the {@code BuildEnvironment} returned by
     *   the Tooling API of Gradle. This argument cannot be {@code null}.
     * @param javaVersion the version of the JVM executing the Gradle daemon.
     *   This argument cannot be {@code null}.
     *
     * @throws NullPointerException thrown if any of the arguments is
     *   {@code null}
     */
    public GradlePlatformInfo(BuildEnvironment buildEnvironment, JavaVersion javaVersion) {
        if (buildEnvironment == null) throw new NullPointerException("buildEnvironment");
        if (javaVersion == null) throw new NullPointerException("javaVersion");

        this.buildEnvironment = buildEnvironment;
        this.javaVersion = javaVersion;
    }

    /**
     * Returns a {@code BuildInfoBuilder} which extracts the
     * {@code GradlePlatformInfo} from the build.
     *
     * @return a {@code BuildInfoBuilder} which extracts the
     *   {@code GradlePlatformInfo} from the build. This method never returns
     *   {@code null}.
     */
    public static BuildInfoBuilder<GradlePlatformInfo> platformBuilder() {
        return GradlePlatformBuilder.INSTANCE;
    }

    /**
     * Returns the {@code BuildEnvironment} returned by the Tooling API of
     * Gradle.
     *
     * @return the {@code BuildEnvironment} returned by the Tooling API of
     *   Gradle. This method may never return {@code null}.
     */
    public BuildEnvironment getBuildEnvironment() {
        return buildEnvironment;
    }

    /**
     * Returns the version of the JVM executing the Gradle daemon.
     *
     * @return the version of the JVM executing the Gradle daemon. This method
     *   may never return {@code null}.
     */
    public JavaVersion getJavaVersion() {
        return javaVersion;
    }

    private enum GradlePlatformBuilder implements BuildInfoBuilder<GradlePlatformInfo> {
        INSTANCE;

        @Override
        public GradlePlatformInfo getInfo(BuildController controller) {
            BuildEnvironment buildEnv = controller.getModel(BuildEnvironment.class);
            JavaVersion javaVersion = JavaVersion.current();

            return new GradlePlatformInfo(buildEnv, javaVersion);
        }

        @Override
        public String getName() {
            return BuilderUtils.getNameForEnumBuilder(this);
        }
    }
}
