package org.netbeans.gradle.project.properties.ui;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.JavaSourcesDisplayMode;
import org.netbeans.gradle.project.util.NbConsumer;

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

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(AppearancePanel.class, new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                displayNamePattern(input).setValue("${project.name}");
                input.javaSourcesDisplayMode().setValue(JavaSourcesDisplayMode.GROUP_BY_SOURCESET);
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(AppearancePanel.class, new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                displayNamePattern(input).setValue("${project.path}-${project.version}-test");
                input.javaSourcesDisplayMode().setValue(JavaSourcesDisplayMode.GROUP_BY_SOURCESET);
            }
        });
    }

    @Test
    public void testInitAndReadBack3() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(AppearancePanel.class, new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                displayNamePattern(input).setValue("${project.path}");
                input.javaSourcesDisplayMode().setValue(JavaSourcesDisplayMode.DEFAULT_MODE);
            }
        });
    }
}
