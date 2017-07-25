package org.netbeans.gradle.project;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jtrim.concurrent.Tasks;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleTaskID;
import org.netbeans.gradle.project.model.NbGenericModelInfo;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.NbGradleMultiProjectDef;
import org.netbeans.gradle.project.model.NbGradleProjectTree;
import org.netbeans.gradle.project.model.SettingsGradleDef;
import org.netbeans.gradle.project.util.LazyPaths;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.NbSupplier;

import static org.junit.Assert.*;

public class DefaultGlobalSettingsFileManagerTest {
    @Rule
    public final TemporaryFolder tmpDir = new TemporaryFolder();

    private Path cacheDir;
    private Path projectsDir;
    private DefaultGlobalSettingsFileManager settingsManager;

    @Before
    public void setUp() throws IOException {
        this.cacheDir = tmpDir.newFolder("cache").toPath();
        this.projectsDir = tmpDir.newFolder("projects").toPath();
        this.settingsManager = new DefaultGlobalSettingsFileManager(new RootProjectRegistry(), new LazyPaths(new NbSupplier<Path>() {
            @Override
            public Path get() {
                return cacheDir;
            }
        }));
    }

    private Runnable waitForOutstandingTask() {
        return new Runnable() {
            @Override
            public void run() {
                settingsManager.waitForOutstanding(5000);
            }
        };
    }

    private void testReadingBackOnlyRoot(Runnable afterUpdate) throws IOException {
        VirtualModelBuilder builder = new VirtualModelBuilder(projectsDir.resolve("test-root"));
        NbGradleModel model = builder.build();

        settingsManager.updateSettingsFile(model);
        afterUpdate.run();

        SettingsGradleDef settingsGradleDef = settingsManager.tryGetSettingsFile(model.getProjectDir());
        assertNotNull(settingsGradleDef);

        assertEquals(model.getSettingsGradleDef(), settingsGradleDef);
    }

    @Test
    public void testReadingBackOnlyRootNoWait() throws IOException {
        testReadingBackOnlyRoot(Tasks.noOpTask());
    }

    @Test
    public void testReadingBackOnlyRoot() throws IOException {
        testReadingBackOnlyRoot(waitForOutstandingTask());
    }

    private void testReadingBackAfterOverwrite(Runnable afterFirstUpdate, Runnable afterSecondUpdate) throws IOException {
        Path subDir = projectsDir.resolve("sub-root").resolve("sub-dir");
        VirtualModelBuilder builder1 = new VirtualModelBuilder(projectsDir.resolve("test-root1"));
        builder1.projectTree().addChild(subDir);
        builder1.setDefaultProjectDir(subDir);
        NbGradleModel model1 = builder1.build();

        VirtualModelBuilder builder2 = new VirtualModelBuilder(projectsDir.resolve("test-root2"));
        builder2.projectTree().addChild(subDir);
        builder2.setDefaultProjectDir(subDir);
        NbGradleModel model2 = builder1.build();

        settingsManager.updateSettingsFile(model1);
        afterFirstUpdate.run();

        settingsManager.updateSettingsFile(model2);
        afterSecondUpdate.run();

        SettingsGradleDef settingsGradleDef = settingsManager.tryGetSettingsFile(subDir.toFile());
        assertNotNull(settingsGradleDef);

        assertEquals(model2.getSettingsGradleDef(), settingsGradleDef);
    }

    @Test
    public void testReadingBackAfterOverwrite1() throws IOException {
        testReadingBackAfterOverwrite(Tasks.noOpTask(), Tasks.noOpTask());
    }

    @Test
    public void testReadingBackAfterOverwrite2() throws IOException {
        testReadingBackAfterOverwrite(Tasks.noOpTask(), waitForOutstandingTask());
    }

    @Test
    public void testReadingBackAfterOverwrite3() throws IOException {
        testReadingBackAfterOverwrite(waitForOutstandingTask(), Tasks.noOpTask());
    }

    @Test
    public void testReadingBackAfterOverwrite4() throws IOException {
        testReadingBackAfterOverwrite(waitForOutstandingTask(), waitForOutstandingTask());
    }

    private static final class VirtualModelBuilder {
        private final TestDebugTree projectTree;
        private Path defaultProjectDir;

        public VirtualModelBuilder(Path rootDir) {
            this.projectTree = new TestDebugTree(rootDir);
            this.defaultProjectDir = rootDir;
        }

        public TestDebugTree projectTree() {
            return projectTree;
        }

        public void setDefaultProjectDir(Path defaultProjectDir) {
            if (projectTree.findSubTree(defaultProjectDir) == null) {
                throw new IllegalArgumentException("No such project: " + defaultProjectDir);
            }

            this.defaultProjectDir = defaultProjectDir;
        }

        public NbGradleModel build() throws IOException {
            NbGradleProjectTree root = projectTree.build("");
            NbGradleProjectTree mainProject = findSubTree(root, defaultProjectDir);

            NbGradleMultiProjectDef projectDef = new NbGradleMultiProjectDef(root, mainProject);
            return new NbGradleModel(
                    new NbGenericModelInfo(projectDef, projectTree.projectDir.resolve("settings.gradle")),
                    Collections.<String, Object>emptyMap(),
                    false);
        }

        private static NbGradleProjectTree findSubTree(NbGradleProjectTree root, Path dir) {
            if (root.getProjectDir().toPath().equals(dir)) {
                return root;
            }

            for (NbGradleProjectTree child: root.getChildren()) {
                NbGradleProjectTree result = findSubTree(child, dir);
                if (result != null) {
                    return result;
                }
            }

            return null;
        }
    }

    private static final class TestDebugTree {
        private final Path projectDir;
        private final List<TestDebugTree> children;

        public TestDebugTree(Path projectDir) {
            this.projectDir = projectDir;
            this.children = new ArrayList<>();
        }

        public TestDebugTree findSubTree(Path dir) {
            if (projectDir.equals(dir)) {
                return this;
            }

            for (TestDebugTree child: children) {
                TestDebugTree result = child.findSubTree(dir);
                if (result != null) {
                    return result;
                }
            }

            return null;
        }

        public TestDebugTree addChild(Path childProjectDir) {
            TestDebugTree childTree = new TestDebugTree(childProjectDir);
            children.add(childTree);
            return childTree;
        }

        public void addChild(Path childProjectDir, NbConsumer<TestDebugTree> childBuilder) {
            childBuilder.accept(addChild(childProjectDir));
        }

        public NbGradleProjectTree build(String pathQualifier) throws IOException {
            Files.createDirectories(projectDir);

            String name = projectDir.getFileName().toString();
            String path = pathQualifier + ":" + name;

            List<NbGradleProjectTree> builtChildren = new ArrayList<>(children.size());
            for (TestDebugTree child: children) {
                builtChildren.add(child.build(path));
            }

            GenericProjectProperties genericProperties = new GenericProjectProperties(
                    name,
                    path,
                    projectDir.toFile(),
                    null);

            return new NbGradleProjectTree(genericProperties, Collections.<GradleTaskID>emptyList(), builtChildren);
        }
    }
}
