package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import java.util.Collections;
import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.standard.CustomVariable;
import org.netbeans.gradle.project.properties.standard.CustomVariables;
import org.netbeans.gradle.project.properties.standard.MemCustomVariables;

public class CustomVariablesPanelTest {
    private static Supplier<ProfileBasedSettingsPage> settingsPageFactory() {
        return CustomVariablesPanel::createSettingsPage;
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGenericInitAndReadBack(settingsPageFactory(), (CommonGlobalSettings input) -> {
            PropertyReference<CustomVariables> customVariables = NbGradleCommonProperties.customVariables(input.getActiveSettingsQuery());
            customVariables.setValue(new MemCustomVariables(Arrays.asList(
                    new CustomVariable("var1", "value1"),
                    new CustomVariable("var2", "value2"))));
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGenericInitAndReadBack(settingsPageFactory(), (CommonGlobalSettings input) -> {
            PropertyReference<CustomVariables> customVariables = NbGradleCommonProperties.customVariables(input.getActiveSettingsQuery());
            customVariables.setValue(new MemCustomVariables(Collections.<CustomVariable>emptyList()));
        });
    }
}
