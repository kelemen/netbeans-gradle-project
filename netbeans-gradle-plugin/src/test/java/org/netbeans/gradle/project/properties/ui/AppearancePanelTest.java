package org.netbeans.gradle.project.properties.ui;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.properties.global.JavaSourcesDisplayMode;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public class AppearancePanelTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static PropertyReference<String> displayNamePattern(CommonGlobalSettings input) {
        return NbGradleCommonProperties.displayNamePattern(input.getActiveSettingsQuery());
    }

    private static NbSupplier<GlobalSettingsPage> settingsPageFactory() {
        return new NbSupplier<GlobalSettingsPage>() {
            @Override
            public GlobalSettingsPage get() {
                return AppearancePanel.createSettingsPage();
            }
        };
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                displayNamePattern(input).setValue("${project.name}");
                input.javaSourcesDisplayMode().setValue(JavaSourcesDisplayMode.GROUP_BY_SOURCESET);
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                displayNamePattern(input).setValue("${project.path}-${project.version}-test");
                input.javaSourcesDisplayMode().setValue(JavaSourcesDisplayMode.GROUP_BY_SOURCESET);
            }
        });
    }

    @Test
    public void testInitAndReadBack3() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                displayNamePattern(input).setValue("${project.path}");
                input.javaSourcesDisplayMode().setValue(JavaSourcesDisplayMode.DEFAULT_MODE);
            }
        });
    }
}
