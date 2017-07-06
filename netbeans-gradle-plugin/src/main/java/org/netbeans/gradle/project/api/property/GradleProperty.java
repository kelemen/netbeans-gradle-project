package org.netbeans.gradle.project.api.property;

import java.nio.charset.Charset;
import org.jtrim2.property.PropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;

/**
 * Contains interfaces for accessing the value of some of the properties on
 * the Gradle properties page (in the project properties). Note that users can
 * only set a single value for each projects of the same multi-project build.
 * If not set by the user, the values of these properties might different for
 * different projects, even if they are within the same multi-project build.
 * <P>
 * Instances of these interfaces are expected to be available on the
 * {@link org.netbeans.api.project.Project#getLookup() project's lookup}. They
 * are already available when loading extensions (i.e., maybe retrieved in the
 * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef#createExtension(org.netbeans.api.project.Project) GradleProjectExtensionDef.createExtension}
 * method.
 */
public final class GradleProperty {
    /**
     * Defines the source file encoding set by the user. If the user does not
     * set the source file encoding, then the default value is "UTF-8".
     * <P>
     * The value of this property is never {@code null}.
     */
    public interface SourceEncoding extends PropertySource<Charset> {
    }

    /**
     * Defines the Java platform used by the Gradle daemon executing Gradle
     * commands.
     * <P>
     * The value of this property should never be {@code null}. Theoretically
     * this property might be {@code null} if {@code JavaPlatform.getDefault()}
     * returns {@code null}. Note however, that in this case something is
     * seriously wrong.
     */
    public interface ScriptPlatform extends PropertySource<JavaPlatform> {
    }

    /**
     * Defines the source level of Java code. Using this property makes little
     * sense for anything but Java projects. Also, expect this property to be
     * deprecated in the future and be replaced by value supplied by the Gradle
     * script. In fact, the user can already choose to inherit this property
     * from the Gradle script.
     * <P>
     * The value of this property is never {@code null}.
     */
    public interface SourceLevel extends PropertySource<String> {
    }

    /**
     * Defines the platform to be used to build the project. Third party code
     * might provide additional platforms (not only what NetBeans provides in
     * "Java Platforms") via the
     * {@link org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery GradleProjectPlatformQuery}.
     * <P>
     * The value of this property is never {@code null}.
     *
     * @see org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery
     */
    public interface BuildPlatform extends PropertySource<ProjectPlatform> {
    }

    private GradleProperty() {
        throw new AssertionError();
    }
}
