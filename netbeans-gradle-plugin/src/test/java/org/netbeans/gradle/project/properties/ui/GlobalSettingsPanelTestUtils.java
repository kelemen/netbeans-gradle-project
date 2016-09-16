package org.netbeans.gradle.project.properties.ui;

import java.util.Collections;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.ui.ProfileEditor;
import org.netbeans.gradle.project.api.config.ui.ProfileInfo;
import org.netbeans.gradle.project.properties.GenericProfileSettings;
import org.netbeans.gradle.project.properties.MultiProfileProperties;
import org.netbeans.gradle.project.properties.SingleProfileSettingsEx;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsEditor;
import org.netbeans.gradle.project.properties.global.SettingsEditorProperties;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public final class GlobalSettingsPanelTestUtils {
    private static CommonGlobalSettings toGlobalSettings(SingleProfileSettingsEx rawSettings) {
        return new CommonGlobalSettings(new MultiProfileProperties(Collections.singletonList(rawSettings)));
    }

    public static void testInitAndReadBack(
            final NbSupplier<? extends GlobalSettingsEditor> panelFactory,
            final NbConsumer<? super CommonGlobalSettings> initializer) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                CommonGlobalSettings.withCleanMemorySettings(new NbConsumer<GenericProfileSettings>() {
                    @Override
                    public void accept(GenericProfileSettings preference) {
                        GenericProfileSettings input = GenericProfileSettings.createTestMemorySettings();
                        CommonGlobalSettings inputSettings = toGlobalSettings(input);

                        GlobalSettingsEditor panel = panelFactory.get();

                        initializer.accept(inputSettings);
                        ConfigTree inputValues = input.getContentSnapshot();

                        ProfileEditor editor = panel.startEditingProfile(
                                new ProfileInfo(ProfileKey.GLOBAL_PROFILE, "Test Settings"),
                                inputSettings.getActiveSettingsQuery());

                        editor.readFromSettings().displaySettings();

                        SettingsEditorProperties properties = panel.getProperties();
                        Assert.assertTrue("Initial form must be valid.", properties.valid().getValue());

                        input.clearSettings();
                        editor.readFromGui().saveSettings();

                        ConfigTree outputValues = input.getContentSnapshot();

                        Assert.assertEquals(inputValues, outputValues);
                    }
                });
            }
        });
    }

    public static void testInitAndReadBack(
            final Class<? extends GlobalSettingsEditor> panelClass,
            NbConsumer<? super CommonGlobalSettings> initializer) throws Exception {

        NbSupplier<GlobalSettingsEditor> panelFactory = new NbSupplier<GlobalSettingsEditor>() {
            @Override
            public GlobalSettingsEditor get() {
                try {
                    return panelClass.newInstance();
                } catch (Exception ex) {
                    throw Exceptions.throwUnchecked(ex);
                }
            }
        };
        testInitAndReadBack(panelFactory, initializer);
    }

    private GlobalSettingsPanelTestUtils() {
        throw new AssertionError();
    }
}
