package org.netbeans.gradle.project.properties.ui;

import java.util.Arrays;
import java.util.List;
import java.util.function.Supplier;
import org.junit.Test;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.standard.PredefinedTasks;

public class ManageTasksPanelTest {
    private static Supplier<ProfileBasedSettingsPage> settingsPageFactory() {
        return ManageTasksPanel::createSettingsPage;
    }

    private static PropertyReference<PredefinedTasks> customTasks(CommonGlobalSettings input) {
        return NbGradleCommonProperties.customTasks(input.getActiveSettingsQuery());
    }

    private void testInitAndReadBack(final PredefinedTasks tasks) throws Exception {
        GlobalSettingsPanelTestUtils.testGenericInitAndReadBack(settingsPageFactory(), (input) -> {
            customTasks(input).setValue(tasks);
        });
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        List<PredefinedTask.Name> tasks = Arrays.asList(
                new PredefinedTask.Name("task1", true),
                new PredefinedTask.Name("task2", false)
        );
        List<String> args = Arrays.asList("arg1", "arg2");
        List<String> jvmArgs = Arrays.asList("jvmarg1", "jvmarg2");
        testInitAndReadBack(new PredefinedTasks(Arrays.asList(
                new PredefinedTask("MyTask1", tasks, args, jvmArgs, true))));
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        List<PredefinedTask.Name> tasks = Arrays.asList(
                new PredefinedTask.Name("task1", true),
                new PredefinedTask.Name("task2", false)
        );
        List<String> args = Arrays.asList();
        List<String> jvmArgs = Arrays.asList();
        testInitAndReadBack(new PredefinedTasks(Arrays.asList(
                new PredefinedTask("MyTask1", tasks, args, jvmArgs, false))));
    }
}
