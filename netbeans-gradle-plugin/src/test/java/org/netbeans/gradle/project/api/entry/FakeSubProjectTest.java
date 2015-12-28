package org.netbeans.gradle.project.api.entry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;
import org.netbeans.junit.MockServices;

import static org.junit.Assert.*;

public final class FakeSubProjectTest {
    private static SampleGradleProject sampleProject;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockServices.setServices();

        GlobalGradleSettings.setCleanMemoryPreference();
        GlobalGradleSettings.getDefault().gradleLocation().setValue(SampleGradleProject.DEFAULT_GRADLE_TARGET);
        GlobalGradleSettings.getDefault().gradleJdk().setValue(JavaPlatform.getDefault());
        GlobalGradleSettings.getDefault().loadRootProjectFirst().setValue(true);

        sampleProject = SampleGradleProject.createProject("without-settings.zip");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        GlobalGradleSettings.setDefaultPreference();
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
