package org.netbeans.gradle.project.util;

import java.util.Arrays;
import org.junit.Rule;
import org.junit.rules.TestRule;
import org.netbeans.gradle.project.api.entry.SampleGradleProject;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;

public abstract class ConfigAwareTest {
    @Rule
    public final TestRule settingsRule;

    public ConfigAwareTest() {
        this(new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings settings) {
            }
        });
    }

    public ConfigAwareTest(final NbConsumer<CommonGlobalSettings> settingsProvider) {
        this.settingsRule = new CustomGlobalSettingsRule(new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings settings) {
                settings.gradleLocation().setValue(SampleGradleProject.DEFAULT_GRADLE_TARGET);
                settings.gradleJvmArgs().setValue(Arrays.asList("-Xmx256m"));
                settings.defaultJdk().setValue(ScriptPlatform.getDefault());

                settingsProvider.accept(settings);
            }
        });
    }
}
