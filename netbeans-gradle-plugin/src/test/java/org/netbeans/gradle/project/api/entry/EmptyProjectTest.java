package org.netbeans.gradle.project.api.entry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.java.nodes.JavaDependenciesNode;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocationVersion;
import org.netbeans.junit.MockServices;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.nodes.Node;

import static org.junit.Assert.*;

public final class EmptyProjectTest {
    private static SampleGradleProject sampleProject;
    private LoadedProject rootProjectRef;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockServices.setServices();
        GlobalGradleSettings.getGradleHome().setValue(new GradleLocationVersion("1.6"));
        GlobalGradleSettings.getGradleJdk().setValue(JavaPlatform.getDefault());

        sampleProject = SampleGradleProject.createProject("empty-project.zip");
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        SampleGradleProject toClose = sampleProject;
        sampleProject = null;

        if (toClose != null) {
            toClose.close();
        }
    }

    @Before
    public void setUp() throws Exception {
        Thread.interrupted();

        rootProjectRef = sampleProject.loadProject("empty-project");

        NbGradleProject project = rootProjectRef.getProject();

        JavaDisablerExtension ext = project.getLookup().lookup(JavaDisablerExtension.class);
        assertNotNull(ext);

        if (!project.tryWaitForLoadedProject(3, TimeUnit.MINUTES)) {
            throw new TimeoutException("Project was not loaded until the timeout elapsed.");
        }
    }

    @After
    public void tearDown() throws Exception {
        rootProjectRef.close();
        rootProjectRef = null;
    }

    @Test
    public void testDisabledJavaExtension() throws Exception {
        NbGradleProject project = rootProjectRef.getProject();

        GradleClassPathProvider cpProvider = project.getLookup().lookup(GradleClassPathProvider.class);
        assertNull("JavaExtension must be disabled and its lookup must not be visible.", cpProvider);
    }

    @Test
    public void testNoDependenciesNode() throws Exception {
        final NbGradleProject project = rootProjectRef.getProject();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                LogicalViewProvider view = project.getLookup().lookup(LogicalViewProvider.class);
                Node root = view.createLogicalView();

                Node[] children = root.getChildren().getNodes();
                for (Node child: children) {
                    if (child instanceof JavaDependenciesNode) {
                        fail("Dependencies node must not be present.");
                    }
                }
            }
        });
    }

    private static void verifyNoJavaActions(Action[] actions) {
        for (Action action: actions) {
            if (action == null) continue;

            Object name = action.getValue("Name");
            if (name == null) continue;

            assertFalse("Must not contain the javadoc command.",
                    "projectCommand:javadoc".equals(name.toString()));
        }
    }

    @Test
    public void testNoJavaActionsAreAdded() throws Exception {
        final NbGradleProject project = rootProjectRef.getProject();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                LogicalViewProvider view = project.getLookup().lookup(LogicalViewProvider.class);
                Node root = view.createLogicalView();

                verifyNoJavaActions(root.getActions(false));
                verifyNoJavaActions(root.getActions(true));
            }
        });
    }
}
