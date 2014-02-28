package org.netbeans.gradle.project.api.entry;

import java.util.Arrays;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.GenericModelFetcher;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.junit.MockServices;

import static org.junit.Assert.*;

public class Latin2ProjectTest {
    private static SampleGradleProject sampleProject;
    private NbGradleProject rootProject;

    @BeforeClass
    public static void setUpClass() throws Exception {
        // Contains all non-ascii Hungarian characters
        String floodTolerantMirrorDrill = "\u00E1rv\u00EDzt\u0171r\u0151t\u00FCk\u00F6rf\u00FAr\u00F3g\u00E9p";
        // Starting with lower case "u" will expand to the unicode escape on Windows.
        // So when this test is run on Windows, it will test this as well.
        GenericModelFetcher.setModelInputPrefix("u-nb-m-input-" + floodTolerantMirrorDrill);

        MockServices.setServices();

        GlobalGradleSettings.getDefault().setAllToDefault();
        GlobalGradleSettings.getGradleHome().setValue(SampleGradleProject.DEFAULT_GRADLE_TARGET);
        GlobalGradleSettings.getGradleJdk().setValue(JavaPlatform.getDefault());
        GlobalGradleSettings.getGradleJvmArgs().setValue(Arrays.asList("-Dfile.encoding=ISO-8859-2", "-Xmx128m"));

        sampleProject = SampleGradleProject.createProject("latin2-project.zip");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        GenericModelFetcher.setDefaultPrefixes();

        SampleGradleProject toClose = sampleProject;
        sampleProject = null;

        if (toClose != null) {
            toClose.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        Thread.interrupted();
        rootProject = sampleProject.loadSingleProject();

        if (!rootProject.tryWaitForLoadedProject(3, TimeUnit.MINUTES)) {
            throw new TimeoutException("Project was not loaded until the timeout elapsed.");
        }
    }

    @After
    public void tearDown() throws Exception {
    }

    private static boolean isJavaExtensionActive(Project project) {
        GradleClassPathProvider javaCpProvider = project.getLookup().lookup(GradleClassPathProvider.class);
        return javaCpProvider != null;
    }

    @Test
    public void testHasLoadedJavaExtension() throws Exception {
        assertTrue("Java extension must be enabled for " + rootProject.getName(),
                isJavaExtensionActive(rootProject));
    }
}