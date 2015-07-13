package org.netbeans.gradle.project.properties.global;

import java.util.Map;
import javax.swing.SwingUtilities;
import org.junit.Assert;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public final class GlobalSettingsPanelTestUtils {
    public static void testInitAndReadBack(
            final NbSupplier<? extends GlobalSettingsEditor> panelFactory,
            final NbConsumer<? super GlobalGradleSettings> initializer) throws Exception {
        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                GlobalSettingsEditor panel = panelFactory.get();

                GlobalGradleSettings.PreferenceContainer preference
                        = GlobalGradleSettings.setCleanMemoryPreference();
                try {
                    GlobalGradleSettings input = new GlobalGradleSettings("input");

                    initializer.accept(input);
                    Map<String, String> inputValues = preference.getKeyValues("input");

                    panel.updateSettings(input);

                    SettingsEditorProperties properties = panel.getProperties();
                    Assert.assertTrue("Initial form must be valid.", properties.valid().getValue());

                    GlobalGradleSettings output = new GlobalGradleSettings("output");

                    panel.saveSettings(output);
                    Map<String, String> outputValues = preference.getKeyValues("output");

                    Assert.assertEquals(inputValues, outputValues);
                } finally {
                    GlobalGradleSettings.setDefaultPreference();
                }
            }
        });
    }

    public static void testInitAndReadBack(
            final Class<? extends GlobalSettingsEditor> panelClass,
            NbConsumer<? super GlobalGradleSettings> initializer) throws Exception {

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
