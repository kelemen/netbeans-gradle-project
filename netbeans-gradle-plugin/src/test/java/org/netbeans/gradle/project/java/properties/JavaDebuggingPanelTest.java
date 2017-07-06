package org.netbeans.gradle.project.java.properties;

import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.ui.GlobalSettingsPanelTestUtils;

public class JavaDebuggingPanelTest {
    private static Supplier<ProfileBasedSettingsPage> settingsPageFactory(final boolean allowInherit) {
        return () -> JavaDebuggingPanel.createSettingsPage(allowInherit);
    }

    private static PropertyReference<DebugMode> javaDebugMode(CommonGlobalSettings input) {
        return JavaProjectProperties.debugMode(input.getActiveSettingsQuery());
    }

    private void testInitAndReadBack(boolean allowInherit, final DebugMode debugMode) throws Exception {
        GlobalSettingsPanelTestUtils.testGenericInitAndReadBack(settingsPageFactory(allowInherit), (input) -> {
            javaDebugMode(input).setValue(debugMode);
        });
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        testInitAndReadBack(false, DebugMode.DEBUGGER_LISTENS);
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        testInitAndReadBack(false, DebugMode.DEBUGGER_ATTACHES);
    }

    @Test
    public void testInitAndReadBack3() throws Exception {
        testInitAndReadBack(true, DebugMode.DEBUGGER_LISTENS);
    }

    @Test
    public void testInitAndReadBack4() throws Exception {
        testInitAndReadBack(true, DebugMode.DEBUGGER_ATTACHES);
    }
}
