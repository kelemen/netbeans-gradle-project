package org.netbeans.gradle.project.properties.global;

import java.io.File;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.GradleLocationDefault;
import org.netbeans.gradle.project.util.NbConsumer;

public class GradleInstallationPanelTest {
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

    @Test
    public void testInitAndReadBack1() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(GradleInstallationPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.gradleLocation().setValue(new GradleLocationDef("1.7", true));
                input.gradleUserHomeDir().setValue(new File("my-user-home1"));
            }
        });
    }

    @Test
    public void testInitAndReadBack2() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(GradleInstallationPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.gradleLocation().setValue(new GradleLocationDef("1.9", false));
                input.gradleUserHomeDir().setValue(new File("my-user-home2"));
            }
        });
    }

    @Test
    public void testInitAndReadBack3() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(GradleInstallationPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.gradleLocation().setValue(new GradleLocationDef(GradleLocationDefault.INSTANCE, false));
                input.gradleUserHomeDir().setValue(new File("my-user-home3"));
            }
        });
    }

    @Test
    public void testInitAndReadBack4() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(GradleInstallationPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.gradleLocation().setValue(new GradleLocationDef(GradleLocationDefault.INSTANCE, true));
                input.gradleUserHomeDir().setValue(new File("my-user-home4"));
            }
        });
    }
}
