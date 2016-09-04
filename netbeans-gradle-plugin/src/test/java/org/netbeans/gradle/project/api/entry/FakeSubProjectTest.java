package org.netbeans.gradle.project.api.entry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.util.ConfigAwareTest;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.junit.MockServices;

import static org.junit.Assert.*;

public final class FakeSubProjectTest extends ConfigAwareTest {
    private static SampleGradleProject sampleProject;

    public FakeSubProjectTest() {
        super(new NbConsumer<CommonGlobalSettings>() {
            @Override
            public void accept(CommonGlobalSettings settings) {
                settings.loadRootProjectFirst().setValue(true);
            }
        });
    }

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockServices.setServices();

        sampleProject = SampleGradleProject.createProject("without-settings.zip");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        SampleGradleProject toClose = sampleProject;
        sampleProject = null;

        if (toClose != null) {
            toClose.close();
        }
    }

    private ProjectId getProjectId(String... projectPath) throws Exception {
        Thread.interrupted();
        NbGradleProject project = sampleProject.loadProject(projectPath);
        if (!project.tryWaitForLoadedProject(3, TimeUnit.MINUTES)) {
            throw new TimeoutException("Project was not loaded until the timeout elapsed.");
        }

        JavaExtension javaExt = project.getLookup().lookup(JavaExtension.class);
        assertNotNull("Lookup must contain entry: JavaExtension", javaExt);

        return javaExt.getCurrentModel().getMainModule().getProperties().getProjectId();
    }

    @Test
    public void testRealRootProject() throws Exception {
        ProjectId id = getProjectId("without-settings");
        assertEquals("group of root", "test-root-without-settings", id.getGroup());
    }

    @Test
    public void testFakeSubProject() throws Exception {
        ProjectId id = getProjectId("without-settings", "fakeSubproject");
        assertEquals("group of root", "fake-subproject", id.getGroup());
    }
}
