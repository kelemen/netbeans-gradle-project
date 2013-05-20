package org.netbeans.gradle.project.api.entry;

import com.google.common.base.Predicate;
import com.google.common.base.Splitter;
import com.google.common.collect.Iterables;
import java.io.Closeable;
import java.io.File;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectManager;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocationVersion;
import org.netbeans.junit.MockServices;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import static org.junit.Assert.*;

/**
 *
 * @author radim
 */
public class GradleProjectExtensionQueryTest {
    private static final List<Closeable> TO_CLOSE = new LinkedList<Closeable>();
    private static File tempFolder;
    private static File prjDir;

    @BeforeClass
    public static void setUpClass() throws Exception {
        MockServices.setServices();

        GlobalGradleSettings.getGradleHome().setValue(new GradleLocationVersion("1.6"));
        GlobalGradleSettings.getGradleJdk().setValue(JavaPlatform.getDefault());

        tempFolder = File.createTempFile("junit", "");
        tempFolder.delete();
        tempFolder.mkdir();
        TestUtils.unzip(GradleProjectExtensionQueryTest.class.getResourceAsStream("gradle-sample.zip"), tempFolder);
        prjDir = FileUtil.normalizeFile(new File(tempFolder, "gradle-sample"));
        TO_CLOSE.add(NbGradleProjectFactory.safeToOpen(prjDir));
    }

    @AfterClass
    public static void tearDownClass() throws Exception {
        for (Closeable closeable : TO_CLOSE) {
            closeable.close();
        }
        TO_CLOSE.clear();
    }

    @AfterClass
    public static void clear() {
        TestUtils.recursiveDelete(tempFolder);
    }

    @Test
    public void basic() throws Exception {
        Project prj = ProjectManager.getDefault().findProject(FileUtil.toFileObject(prjDir));
        NbGradleProject gPrj = prj.getLookup().lookup(NbGradleProject.class);
        assertNotNull(prj);
        GradleTestExtension ext = gPrj.getLookup().lookup(GradleTestExtension.class);
        assertNotNull(ext);
        ext.loadedSignal.await(150, TimeUnit.SECONDS);

        FileObject foProjectSrc = prj.getProjectDirectory().getFileObject("src/main/java/org/netbeans/gradle/Sample.java");

        // get the classpath
        verifyClasspath(prj, foProjectSrc, ClassPath.SOURCE, "gradle-sample/src/main/java");
        // need to add some test here
        // verifyClasspath(prj, foProjectSrc, ClassPath.BOOT, "android.jar", "annotations.jar");
    }

    private void verifyClasspath(Project prj, FileObject fo, String cpType, String... entries) {
        ClassPathProvider cpp = prj.getLookup().lookup(ClassPathProvider.class);
        ClassPath classpath = cpp.findClassPath(fo, cpType);
        assertNotNull("classpath " + cpType + " found", classpath);

        for (final String entry : entries) {
            assertTrue(
                    "classpath " + classpath + "contains entry " + entry,
                    Iterables.any(
                    Splitter.on(':').split(classpath.toString()),
                    new Predicate<String>() {
                @Override
                public boolean apply(String t) {
                    return t.endsWith(entry);
                }
            }));
        }
    }
}