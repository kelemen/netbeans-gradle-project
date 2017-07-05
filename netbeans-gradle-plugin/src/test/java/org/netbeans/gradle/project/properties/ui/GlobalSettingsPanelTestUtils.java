package org.netbeans.gradle.project.properties.ui;

import java.lang.reflect.InvocationTargetException;
import java.util.Collections;
import javax.swing.SwingUtilities;
import org.jtrim.utils.ExceptionHelper;
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

        CommonGlobalSettings.withCleanMemorySettings((GenericProfileSettings preference) -> {
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
        });
    }

    private static void invokeAndWait(Runnable task) throws InterruptedException {
        if (SwingUtilities.isEventDispatchThread()) {
            task.run();
        }
        else {
            try {
                SwingUtilities.invokeAndWait(task);
            } catch (InvocationTargetException ex) {
                Throwable cause = ex.getCause();
                if (cause == null) {
                    cause = ex;
                }
                throw ExceptionHelper.throwUnchecked(cause);
            }
        }
    }

    public static <T extends ProfileBasedSettingsPage> void testInitAndReadBack(
            final NbSupplier<? extends T> panelFactory,
            final NbConsumer<? super CommonGlobalSettings> initializer,
            final NbConsumer<? super T> check) throws Exception {
        invokeAndWait(() -> {
            testInitAndReadBackNow(panelFactory, initializer, check);
        });
    }

    public static void testGenericInitAndReadBack(
            final NbSupplier<? extends ProfileBasedSettingsPage> pageFactory,
            final NbConsumer<? super CommonGlobalSettings> initializer) throws Exception {
        testInitAndReadBack(pageFactory, initializer, page -> { });
    }

    public static void testGlobalInitAndReadBack(
            final NbSupplier<? extends GlobalSettingsPage> pageFactory,
            final NbConsumer<? super CommonGlobalSettings> initializer) throws Exception {
        testInitAndReadBack(pageFactory, initializer, (GlobalSettingsPage page) -> {
            Assert.assertTrue("Initial form must be valid.", page.valid().getValue());
        });
    }

    private GlobalSettingsPanelTestUtils() {
        throw new AssertionError();
    }
}
