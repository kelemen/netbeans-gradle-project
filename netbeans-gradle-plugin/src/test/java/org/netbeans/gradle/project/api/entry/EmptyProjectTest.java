package org.netbeans.gradle.project.api.entry;

import java.util.concurrent.TimeUnit;
import javax.swing.Action;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.java.nodes.JavaDependenciesNode;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.util.SwingTest;
import org.netbeans.gradle.project.util.SwingTestAware;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.nodes.Node;

import static org.junit.Assert.*;

public final class EmptyProjectTest extends SwingTestAware {
    private static final String RESOURCE_BASE = "/" + EmptyProjectTest.class.getPackage().getName().replace('.', '/');
    public static final String EMPTY_PROJECT_RESOURCE = RESOURCE_BASE + "/empty-project.zip";
    public static final String EMPTY_PROJECT_NAME = "empty-project";

    @ClassRule
    public static final SampleProjectRule PROJECT_REF = SampleProjectRule.getStandardRule(EMPTY_PROJECT_RESOURCE,
            SingleModelExtensionQuery.class);

    private NbGradleProject rootProject;

    @Before
    public void setUp() throws Exception {
        rootProject = PROJECT_REF.loadAndWaitProject(EMPTY_PROJECT_NAME);

        GradleTestExtension ext = rootProject.getLookup().lookup(GradleTestExtension.class);
        assertNotNull(ext);
    }

    @Test
    public void testOldAPIFetchedModel() throws Exception {
        SingleModelExtension<?> extension = rootProject.getLookup().lookup(SingleModelExtension.class);
        assertNotNull(extension);

        Object model = extension.getModel(60, TimeUnit.SECONDS);
        assertTrue("Must have retrieved the eclipse project model.", model instanceof EclipseProject);
    }

    @Test
    public void testDisabledJavaExtension() throws Exception {
        GradleClassPathProvider cpProvider = rootProject.getLookup().lookup(GradleClassPathProvider.class);
        assertNull("JavaExtension must be disabled and its lookup must not be visible.", cpProvider);
    }

    @Test
    @SwingTest
    public void testNoDependenciesNode() throws Exception {
        LogicalViewProvider view = rootProject.getLookup().lookup(LogicalViewProvider.class);
        Node root = view.createLogicalView();

        Node[] children = root.getChildren().getNodes();
        for (Node child: children) {
            if (child instanceof JavaDependenciesNode) {
                fail("Dependencies node must not be present.");
            }
        }
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
    @SwingTest
    public void testNoJavaActionsAreAdded() throws Exception {
        LogicalViewProvider view = rootProject.getLookup().lookup(LogicalViewProvider.class);
        Node root = view.createLogicalView();

        verifyNoJavaActions(root.getActions(false));
        verifyNoJavaActions(root.getActions(true));
    }
}
