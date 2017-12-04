package org.netbeans.gradle.project.properties.ui;

import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.global.SelfMaintainedTasks;

public class TaskExecutionPanelTest {
    private static Supplier<GlobalSettingsPage> settingsPageFactory() {
        return TaskExecutionPanel::createSettingsPage;
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.selfMaintainedTasks().setValue(SelfMaintainedTasks.TRUE);
            input.alwaysClearOutput().setValue(true);
            input.skipTests().setValue(true);
            input.skipCheck().setValue(false);
            input.replaceLfOnStdIn().setValue(false);
            input.askBeforeCancelExec().setValue(false);
            input.showGradleVersion().setValue(true);
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.selfMaintainedTasks().setValue(SelfMaintainedTasks.FALSE);
            input.alwaysClearOutput().setValue(false);
            input.skipTests().setValue(false);
            input.skipCheck().setValue(true);
            input.replaceLfOnStdIn().setValue(true);
            input.askBeforeCancelExec().setValue(false);
            input.showGradleVersion().setValue(false);
        });
    }

    @Test
    public void testInitAndReadBack3() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), (input) -> {
            input.selfMaintainedTasks().setValue(SelfMaintainedTasks.MANUAL);
            input.alwaysClearOutput().setValue(true);
            input.skipTests().setValue(false);
            input.skipCheck().setValue(true);
            input.replaceLfOnStdIn().setValue(false);
            input.askBeforeCancelExec().setValue(false);
            input.showGradleVersion().setValue(true);
        });
    }
}
