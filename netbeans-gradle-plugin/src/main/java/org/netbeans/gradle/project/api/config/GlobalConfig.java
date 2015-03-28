package org.netbeans.gradle.project.api.config;

import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.property.NbPropertySource;
import org.netbeans.gradle.project.properties.NbPropertySourceWrapper;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;

/**
 * Contains the values of some of the properties set in the global settings.
 */
public final class GlobalConfig {
    private static final NbPropertySource<Boolean> SKIP_TESTS
            = new NbPropertySourceWrapper<>(GlobalGradleSettings.getDefault().skipTests());

    private static final NbPropertySource<Boolean> SKIP_CHECK
            = new NbPropertySourceWrapper<>(GlobalGradleSettings.getDefault().skipCheck());

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
    public static NbPropertySource<Boolean> skipTests() {
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
    public static NbPropertySource<Boolean> skipCheck() {
      return SKIP_CHECK;
    }

    private GlobalConfig() {
        throw new AssertionError();
    }
}
