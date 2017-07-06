package org.netbeans.gradle.project.util;

import java.util.Objects;
import java.util.function.Consumer;
import org.junit.rules.TestRule;
import org.junit.runner.Description;
import org.junit.runners.model.Statement;
import org.netbeans.gradle.project.properties.GenericProfileSettings;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;

public final class CustomGlobalSettingsRule implements TestRule {
    private final Consumer<? super CommonGlobalSettings> settingsProvider;

    public CustomGlobalSettingsRule(Consumer<? super CommonGlobalSettings> settingsProvider) {
        this.settingsProvider = Objects.requireNonNull(settingsProvider, "settingsProvider");
    }

    @Override
    public Statement apply(final Statement base, Description description) {
        return new Statement() {
            @Override
            public void evaluate() throws Throwable {
                withCleanMemorySettings(base);
            }
        };
    }

    private void withCleanMemorySettings(final Statement base) throws Throwable {
        withCleanMemorySettings((GenericProfileSettings settings) -> {
            settingsProvider.accept(CommonGlobalSettings.getDefault());

            try {
                base.evaluate();
            } catch (Throwable ex) {
                throw new TestExceptionWrapper(ex);
            }
        });
    }

    private static void withCleanMemorySettings(Consumer<GenericProfileSettings> task) throws Throwable {
        try {
            CommonGlobalSettings.withCleanMemorySettings(task);
        } catch (TestExceptionWrapper ex) {
            throw ex.getCause();
        }
    }

    private static class TestExceptionWrapper extends RuntimeException {
        private static final long serialVersionUID = 1L;

        public TestExceptionWrapper(Throwable cause) {
            super(cause);
        }
    }
}
