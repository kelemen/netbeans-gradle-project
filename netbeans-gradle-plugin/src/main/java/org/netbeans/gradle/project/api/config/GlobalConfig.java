package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.properties.ExtensionActiveSettingsQuery;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;

/**
 * Contains the values of some of the properties set in the global settings.
 */
public final class GlobalConfig {
    private static final PropertySource<Boolean> SKIP_TESTS
            = CommonGlobalSettings.getDefault().skipTests().getActiveSource();

    private static final PropertySource<Boolean> SKIP_CHECK
            = CommonGlobalSettings.getDefault().skipCheck().getActiveSource();

    /**
     * Returns a settings container for global settings associated with the specified extension
     * of this Gradle plugin. The extensions must be identified by a globally unique string
     * which is preferably the name of the extension as defined by
     * {@link org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef GradleProjectExtensionDef}.
     * Note that this name should also be the same as provided for {@link ProjectSettingsProvider},
     * otherwise project properties will fail to inherit from the global properties.
     *
     * @param extensionName the string identifying the extension in the
     *   configuration. The preferred convention is to use the extension's name,
     *   though it is not strictly required. This argument cannot be {@code null}.
     * @return the global settings of the requested extension. This method
     *   never returns {@code null}.
     */
    public static ActiveSettingsQuery getGlobalSettingsQuery(String extensionName) {
        ActiveSettingsQuery rootQuery = CommonGlobalSettings.getDefaultActiveSettingsQuery();
        return new ExtensionActiveSettingsQuery(rootQuery, extensionName);
    }

    /**
     * Returns the property indicating if tests should be skipped when executing
     * tasks not directly related to testing (such as build). Extensions should
     * consider this value when providing their own
     * {@link org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery custom built-in tasks}.
     * <P>
     * The value of the returned property is never {@code null}.
     *
     * @return the property indicating if tests should be skipped when executing
     *   tasks not directly related to testing (such as build). This method
     *   never returns {@code null}.
     */
    @Nonnull
    public static PropertySource<Boolean> skipTests() {
        return SKIP_TESTS;
    }

    /**
     * Returns the property indicating if the check task should be skipped when executing
     * tasks not directly related to check (such as build). Extensions should
     * consider this value when providing their own
     * {@link org.netbeans.gradle.project.api.task.BuiltInGradleCommandQuery custom built-in tasks}.
     * <P>
     * The value of the returned property is never {@code null}.
     *
     * @return the property indicating if the check task should be skipped when executing
     *   tasks not directly related to testing (such as build). This method
     *   never returns {@code null}.
     */
    @Nonnull
    public static PropertySource<Boolean> skipCheck() {
      return SKIP_CHECK;
    }

    private GlobalConfig() {
        throw new AssertionError();
    }
}
