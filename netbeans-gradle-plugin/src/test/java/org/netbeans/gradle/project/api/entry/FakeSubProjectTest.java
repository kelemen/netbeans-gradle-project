package org.netbeans.gradle.project.api.entry;

import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.gradle.model.ProjectId;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.util.NbConsumer;

import static org.junit.Assert.*;

public final class FakeSubProjectTest {
    private static final NbConsumer<CommonGlobalSettings> EXTRA_SETTINGS = new NbConsumer<CommonGlobalSettings>() {
        @Override
        public void accept(CommonGlobalSettings settings) {
            settings.loadRootProjectFirst().setValue(true);
        }
    };

    @ClassRule
    public static final SampleProjectRule PROJECT_REF = SampleProjectRule.getStandardRule("without-settings.zip", EXTRA_SETTINGS);

    private ProjectId getProjectId(String... projectPath) throws Exception {
        NbGradleProject project = PROJECT_REF.loadAndWaitProject(projectPath);

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
