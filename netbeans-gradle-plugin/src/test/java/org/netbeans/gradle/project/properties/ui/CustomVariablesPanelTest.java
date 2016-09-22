package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import java.util.Collections;
import org.junit.Test;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.standard.CustomVariable;
import org.netbeans.gradle.project.properties.standard.CustomVariables;
import org.netbeans.gradle.project.properties.standard.MemCustomVariables;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public class CustomVariablesPanelTest {
    private static NbSupplier<ProfileBasedSettingsPage> settingsPageFactory() {
        return new NbSupplier<ProfileBasedSettingsPage>() {
            @Override
            public ProfileBasedSettingsPage get() {
                return CustomVariablesPanel.createSettingsPage();
            }
        };
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGenericInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                PropertyReference<CustomVariables> customVariables = NbGradleCommonProperties.customVariables(input.getActiveSettingsQuery());
                customVariables.setValue(new MemCustomVariables(Arrays.asList(
                        new CustomVariable("var1", "value1"),
                        new CustomVariable("var2", "value2"))));
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGenericInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                PropertyReference<CustomVariables> customVariables = NbGradleCommonProperties.customVariables(input.getActiveSettingsQuery());
                customVariables.setValue(new MemCustomVariables(Collections.<CustomVariable>emptyList()));
            }
        });
    }
}
