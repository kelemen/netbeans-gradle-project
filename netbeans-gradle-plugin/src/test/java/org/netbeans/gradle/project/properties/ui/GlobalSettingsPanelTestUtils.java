package org.netbeans.gradle.project.properties.ui;

import java.util.Collections;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.properties.GenericProfileSettings;
import org.netbeans.gradle.project.properties.MultiProfileProperties;
import org.netbeans.gradle.project.properties.SingleProfileSettingsEx;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public final class GlobalSettingsPanelTestUtils {
    private static CommonGlobalSettings toGlobalSettings(SingleProfileSettingsEx rawSettings) {
        return new CommonGlobalSettings(new MultiProfileProperties(Collections.singletonList(rawSettings)));
    }

    private static <T extends ProfileBasedSettingsPage> void testInitAndReadBackNow(
            final NbSupplier<? extends T> pageFactory,
            final NbConsumer<? super CommonGlobalSettings> initializer,
            final NbConsumer<? super T> check) {
        if (!SwingUtilities.isEventDispatchThread()) {
            throw new IllegalStateException("Expected EDT");
        }

        CommonGlobalSettings.withCleanMemorySettings(new NbConsumer<GenericProfileSettings>() {
            @Override
            public void accept(GenericProfileSettings preference) {
                GenericProfileSettings input = GenericProfileSettings.createTestMemorySettings();
                CommonGlobalSettings inputSettings = toGlobalSettings(input);

                T page = pageFactory.get();

                initializer.accept(inputSettings);
                ConfigTree inputValues = input.getContentSnapshot();

                ProfileEditor editor = page.getEditorFactory().startEditingProfile(
                        new ProfileInfo(ProfileKey.GLOBAL_PROFILE, NbStrings.getGlobalProfileName()),
                        inputSettings.getActiveSettingsQuery());

                editor.readFromSettings().displaySettings();

                check.accept(page);

                input.clearSettings();
                editor.readFromGui().saveSettings();

                ConfigTree outputValues = input.getContentSnapshot();

                Assert.assertEquals(inputValues, outputValues);
            }
        });
    }

    public static <T extends ProfileBasedSettingsPage> void testInitAndReadBack(
            final NbSupplier<? extends T> panelFactory,
            final NbConsumer<? super CommonGlobalSettings> initializer,
            final NbConsumer<? super T> check) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                testInitAndReadBackNow(panelFactory, initializer, check);
            }
        });
    }

    public static void testGenericInitAndReadBack(
            final NbSupplier<? extends ProfileBasedSettingsPage> pageFactory,
            final NbConsumer<? super CommonGlobalSettings> initializer) throws Exception {
        testInitAndReadBack(pageFactory, initializer, new NbConsumer<ProfileBasedSettingsPage>() {
            @Override
            public void accept(ProfileBasedSettingsPage page) {
            }
        });
    }

    public static void testGlobalInitAndReadBack(
            final NbSupplier<? extends GlobalSettingsPage> pageFactory,
            final NbConsumer<? super CommonGlobalSettings> initializer) throws Exception {
        testInitAndReadBack(pageFactory, initializer, new NbConsumer<GlobalSettingsPage>() {
            @Override
            public void accept(GlobalSettingsPage page) {
                Assert.assertTrue("Initial form must be valid.", page.valid().getValue());
            }
        });
    }

    private GlobalSettingsPanelTestUtils() {
        throw new AssertionError();
    }
}
