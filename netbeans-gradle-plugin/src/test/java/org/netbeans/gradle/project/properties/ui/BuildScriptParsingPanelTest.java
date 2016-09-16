package org.netbeans.gradle.project.properties.ui;

import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.properties.ModelLoadingStrategy;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.GlobalSettingsPage;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

public class BuildScriptParsingPanelTest {
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

    private static NbSupplier<GlobalSettingsPage> settingsPageFactory() {
        return new NbSupplier<GlobalSettingsPage>() {
            @Override
            public GlobalSettingsPage get() {
                return BuildScriptParsingPanel.createSettingsPage();
            }
        };
    }

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                input.modelLoadingStrategy().setValue(ModelLoadingStrategy.USE_IDEA_MODEL);
                input.loadRootProjectFirst().setValue(false);
                input.mayRelyOnJavaOfScript().setValue(true);
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testGlobalInitAndReadBack(settingsPageFactory(), new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings input) {
                input.modelLoadingStrategy().setValue(ModelLoadingStrategy.NEWEST_POSSIBLE);
                input.loadRootProjectFirst().setValue(true);
                input.mayRelyOnJavaOfScript().setValue(false);
            }
        });
    }
}
