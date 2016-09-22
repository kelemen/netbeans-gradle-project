package org.netbeans.gradle.project.properties.ui;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import org.junit.Test;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;


public class LicenseHeaderPanelTest {
    private static final NbSupplier<Path> NULL_PATH = new NbSupplier<Path>() {
        @Override
        public Path get() {
            return null;
        }
    };

    private static NbSupplier<ProfileBasedSettingsPage> settingsPageFactory() {
        return new NbSupplier<ProfileBasedSettingsPage>() {
            @Override
            public ProfileBasedSettingsPage get() {
                return LicenseHeaderPanel.createSettingsPage(NULL_PATH);
            }
        };
    }

    private static PropertyReference<LicenseHeaderInfo> licenseHeaderInfo(CommonGlobalSettings input) {
        return NbGradleCommonProperties.licenseHeaderInfo(input.getActiveSettingsQuery());
    }

    private void testInitAndReadBack(final LicenseHeaderInfo headerInfo) throws Exception {
        GlobalSettingsPanelTestUtils.testGenericInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                licenseHeaderInfo(input).setValue(headerInfo);
            }
        });
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        testInitAndReadBack(null);
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        testInitAndReadBack(new LicenseHeaderInfo(
                "MyLicense",
                Collections.singletonMap("organization", "MyTestOrg"),
                Paths.get("MyLicense.txt")));
    }

    @Test
    public void testInitAndReadBack3() throws Exception {
        testInitAndReadBack(new LicenseHeaderInfo(
                "MyLicense",
                Collections.singletonMap("organization", "MyTestOrg"),
                null));
    }
}
