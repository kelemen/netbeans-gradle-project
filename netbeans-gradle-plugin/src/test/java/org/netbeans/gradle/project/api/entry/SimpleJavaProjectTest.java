package org.netbeans.gradle.project.api.entry;

import com.google.common.base.Predicate;
import com.google.common.collect.Iterables;
import java.io.Closeable;
import java.io.File;
import java.util.Arrays;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.model.util.ZipUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocationVersion;
import org.netbeans.junit.MockServices;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.project.ActionProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

import static org.junit.Assert.*;
import static org.netbeans.spi.project.ActionProvider.*;

/**
 *
 * @author radim
 */
public class SimpleJavaProjectTest {
    private static final List<Closeable> TO_CLOSE = new LinkedList<Closeable>();
    private static File tempFolder;
    private static File projectDir;
    private Project project;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockServices.setServices();
        GlobalGradleSettings.getGradleHome().setValue(new GradleLocationVersion("1.6"));
        GlobalGradleSettings.getGradleJdk().setValue(JavaPlatform.getDefault());

        tempFolder = ZipUtils.unzipResourceToTemp(SimpleJavaProjectTest.class, "gradle-sample.zip");
        projectDir = FileUtil.normalizeFile(new File(tempFolder, "gradle-sample"));
        TO_CLOSE.add(NbGradleProjectFactory.safeToOpen(projectDir));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        for (Closeable closeable : TO_CLOSE) {
            closeable.close();
        }
        TO_CLOSE.clear();
    }

    @Before
    public void setUp() throws Exception {
        Thread.interrupted();
        project = ProjectManager.getDefault().findProject(FileUtil.toFileObject(projectDir));
        NbGradleProject gPrj = project.getLookup().lookup(NbGradleProject.class);
        assertNotNull(project);
        GradleTestExtension ext = gPrj.getLookup().lookup(GradleTestExtension.class);
        assertNotNull(ext);
        if (!ext.loadedSignal.await(150, TimeUnit.SECONDS)) {
            throw new TimeoutException("Project was not loaded until the timeout elapsed.");
        }
    }

    @After
    public void tearDown() {
        project = null;
    }

    @AfterClass
    public static void clear() throws Exception {
        ZipUtils.recursiveDelete(tempFolder);
    }

    @Test
    public void testClassPath() throws Exception {
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
    public void testSingleCommandsEnabledForJava() {
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
}