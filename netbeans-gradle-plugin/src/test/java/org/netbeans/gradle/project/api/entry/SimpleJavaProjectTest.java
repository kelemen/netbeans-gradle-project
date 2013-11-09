package org.netbeans.gradle.project.api.entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.java.nodes.JavaDependenciesNode;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocationVersion;
import org.netbeans.gradle.project.view.BuildScriptsNode;
import org.netbeans.junit.MockServices;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.openide.filesystems.FileObject;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

import static org.junit.Assert.*;
import static org.netbeans.spi.project.ActionProvider.*;

/**
 *
 * @author radim
 */
public class SimpleJavaProjectTest {
    private static SampleGradleProject sampleProject;
    private LoadedProject rootProjectRef;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockServices.setServices();
        GlobalGradleSettings.getGradleHome().setValue(new GradleLocationVersion("1.6"));
        GlobalGradleSettings.getGradleJdk().setValue(JavaPlatform.getDefault());

        sampleProject = SampleGradleProject.createProject("gradle-sample.zip");
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

        rootProjectRef = sampleProject.loadProject("gradle-sample");

        NbGradleProject project = rootProjectRef.getProject();

        GradleTestExtension ext = project.getLookup().lookup(GradleTestExtension.class);
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
    public void testClassPath() throws Exception {
        NbGradleProject project = rootProjectRef.getProject();

        FileObject foProjectSrc = project.getProjectDirectory().getFileObject("src/main/java/org/netbeans/gradle/Sample.java");

        // get the classpath
        verifyClasspath(project, foProjectSrc, ClassPath.SOURCE, "gradle-sample/src/main/java");
        // need to add some test here
        // verifyClasspath(prj, foProjectSrc, ClassPath.BOOT, "android.jar", "annotations.jar");
    }

    private static String[] getSingleCommands() {
        return new String[] {
            COMMAND_RUN_SINGLE,
            COMMAND_DEBUG_SINGLE,
            COMMAND_TEST_SINGLE,
            COMMAND_DEBUG_TEST_SINGLE,
        };
    }

    @Test
    public void testSingleCommandsEnabledForJava() throws Exception {
        NbGradleProject project = rootProjectRef.getProject();

        ActionProvider actionProvider = project.getLookup().lookup(ActionProvider.class);
        Set<String> supportedActions = new HashSet<String>(Arrays.asList(actionProvider.getSupportedActions()));

        FileObject javaFile = project.getProjectDirectory().getFileObject(
                "src/main/java/org/netbeans/gradle/Sample.java");
        for (String command: getSingleCommands()) {
            Lookup lookup = Lookups.fixed(javaFile);
            boolean actionEnabled = actionProvider.isActionEnabled(command, lookup);
            assertTrue(actionEnabled);
            assertTrue(supportedActions.contains(command));
        }
    }

    private Set<String> getRootsOfClassPath(ClassPath classpath) {
        Set<String> roots = new HashSet<String>();
        for (FileObject cpEntry: classpath.getRoots()) {
            roots.add(cpEntry.getPath());
        }
        return roots;
    }

    private void verifyClasspath(Project prj, FileObject fo, String cpType, String... entries) {
        ClassPathProvider cpp = prj.getLookup().lookup(ClassPathProvider.class);
        ClassPath classpath = cpp.findClassPath(fo, cpType);
        assertNotNull("classpath " + cpType + " found", classpath);

        Set<String> cpRoots = getRootsOfClassPath(classpath);
        for (final String entry: entries) {
            assertTrue(
                    "classpath " + classpath + " contains entry " + entry,
                    Iterables.any(cpRoots, new Predicate<String>() {
                @Override
                public boolean apply(String t) {
                    return t.endsWith(entry);
                }
            }));
        }
    }

    @Test
    public void testHasProperNodes() throws Exception {
        final NbGradleProject project = rootProjectRef.getProject();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                LogicalViewProvider view = project.getLookup().lookup(LogicalViewProvider.class);
                Node root = view.createLogicalView();

                Lookup children = Lookups.fixed((Object[])root.getChildren().getNodes());
                JavaDependenciesNode dependenciesNode = children.lookup(JavaDependenciesNode.class);
                BuildScriptsNode buildScriptsNode = children.lookup(BuildScriptsNode.class);

                assertNotNull("Must have a dependencies node", dependenciesNode);
                assertNotNull("Must have a build scripts node", buildScriptsNode);
            }
        });
    }

    private static void verifyJavaDocActionIsAdded(Action[] actions) {
        for (Action action: actions) {
            if (action == null) continue;

            Object name = action.getValue("Name");
            if (name == null) continue;

            if ("projectCommand:javadoc".equals(name.toString())) {
                return;
            }
        }

        fail("Could not find javadoc command.");
    }

    @Test
    public void testJavadocActionIsAdded() throws Exception {
        final NbGradleProject project = rootProjectRef.getProject();

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                LogicalViewProvider view = project.getLookup().lookup(LogicalViewProvider.class);
                Node root = view.createLogicalView();

                verifyJavaDocActionIsAdded(root.getActions(false));
                verifyJavaDocActionIsAdded(root.getActions(true));
            }
        });
    }
}