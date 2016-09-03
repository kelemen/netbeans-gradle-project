package org.netbeans.gradle.project.properties.ui;

import java.util.concurrent.TimeUnit;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.netbeans.gradle.project.util.NbConsumer;


public class GradleDaemonPanelTest {
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
    public void testInitAndReadBack() throws Exception {
        GlobalSettingsPanelTestUtils.testInitAndReadBack(GradleDaemonPanel.class, new NbConsumer<GlobalGradleSettings>() {
            @Override
            public void accept(GlobalGradleSettings input) {
                input.gradleDaemonTimeoutSec().setValue((int)TimeUnit.MINUTES.toSeconds(37));
            }
        });
    }
}
