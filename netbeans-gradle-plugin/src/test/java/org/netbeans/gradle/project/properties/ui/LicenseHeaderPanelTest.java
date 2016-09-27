package org.netbeans.gradle.project.properties.ui;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import org.junit.Test;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.config.ui.ProfileBasedSettingsPage;
import org.netbeans.gradle.project.license.LicenseHeaderInfo;
import org.netbeans.gradle.project.license.LicenseRef;
import org.netbeans.gradle.project.license.LicenseSource;
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

    private static final LicenseSource EMPTY_LICENSE_SOURCE = licenseSource(Collections.<LicenseRef>emptyList());

    private static LicenseSource licenseSource(final Collection<LicenseRef> licenses) {
        return new LicenseSource() {
            @Override
            public Collection<LicenseRef> getAllLicense() throws IOException {
                return licenses;
            }
        };
    }

    private static NbSupplier<ProfileBasedSettingsPage> settingsPageFactory(final LicenseSource licenseSource) {
        return new NbSupplier<ProfileBasedSettingsPage>() {
            @Override
            public ProfileBasedSettingsPage get() {
                return LicenseHeaderPanel.createSettingsPage(NULL_PATH, licenseSource);
            }
        };
    }

    private static NbSupplier<ProfileBasedSettingsPage> settingsPageFactory() {
        return settingsPageFactory(EMPTY_LICENSE_SOURCE);
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

    private void testInitAndReadBackWithKnownLicense(boolean dynamic) throws Exception {
        licenseSource(Arrays.asList(
                new LicenseRef("AAAA", "DisplayNameOfAAA", false),
                new LicenseRef("MyBuiltInLicense", "DisplayNameOfLicense", dynamic),
                new LicenseRef("ZZZZ", "DisplayNameOfZZZZ", false)
        ));
        GlobalSettingsPanelTestUtils.testGenericInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                LicenseHeaderInfo headerInfo = new LicenseHeaderInfo(
                        "MyBuiltInLicense",
                        Collections.singletonMap("organization", "MyTestOrg"),
                        null);
                licenseHeaderInfo(input).setValue(headerInfo);
            }
        });
    }

    @Test
    public void testInitAndReadBackWithKnownLicense1() throws Exception {
        testInitAndReadBackWithKnownLicense(true);
    }

    @Test
    public void testInitAndReadBackWithKnownLicense2() throws Exception {
        testInitAndReadBackWithKnownLicense(false);
    }
}
