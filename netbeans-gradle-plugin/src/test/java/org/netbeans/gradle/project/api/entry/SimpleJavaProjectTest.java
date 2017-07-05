package org.netbeans.gradle.project.api.entry;

import com.google.common.collect.Iterables;
import java.beans.PropertyChangeListener;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import javax.swing.Action;
import javax.swing.Icon;
import javax.swing.event.ChangeListener;
import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Test;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.project.JavaProjectConstants;
import org.netbeans.api.project.Project;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.api.project.SourceGroup;
import org.netbeans.api.project.Sources;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectIssueManager;
import org.netbeans.gradle.project.api.property.GradleProperty;
import org.netbeans.gradle.project.api.task.GradleCommandExecutor;
import org.netbeans.gradle.project.java.nodes.JavaDependenciesNode;
import org.netbeans.gradle.project.java.nodes.JavaExtensionNodes;
import org.netbeans.gradle.project.java.nodes.JavaProjectContextActions;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.java.tasks.GradleJavaBuiltInCommands;
import org.netbeans.gradle.project.util.SwingTest;
import org.netbeans.gradle.project.util.SwingTestAware;
import org.netbeans.gradle.project.view.BuildScriptsNode;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.netbeans.spi.project.ProjectState;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.PrivilegedTemplates;
import org.netbeans.spi.project.ui.RecommendedTemplates;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

import static org.junit.Assert.*;
import static org.netbeans.spi.project.ActionProvider.*;

@SuppressWarnings("deprecation")
public class SimpleJavaProjectTest extends SwingTestAware {
    @ClassRule
    public static final SampleProjectRule PROJECT_REF = SampleProjectRule.getStandardRule("gradle-sample.zip",
            CustomSourcesMergerExtDef.class);

    private NbGradleProject rootProject;

    public SimpleJavaProjectTest() {
    }

    @Before
    public void setUp() throws Exception {
        rootProject = PROJECT_REF.loadAndWaitProject("gradle-sample");

        GradleTestExtension ext = rootProject.getLookup().lookup(GradleTestExtension.class);
        assertNotNull(ext);
    }

    @Test
    public void testProjectServiceProvider() throws Exception {
        Project project = rootProject;

        MyCustomLookupEntry entry = project.getLookup().lookup(MyCustomLookupEntry.class);
        assertNotNull("Lookup must contain entry: MyCustomLookupEntry", entry);
        assertEquals("Must be registered with the currect project.",
                project.getProjectDirectory(),
                entry.getProject().getProjectDirectory());
    }

    @Test
    public void testClassPath() throws Exception {
        NbGradleProject project = rootProject;

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
        NbGradleProject project = rootProject;

        ActionProvider actionProvider = project.getLookup().lookup(ActionProvider.class);
        Set<String> supportedActions = new HashSet<>(Arrays.asList(actionProvider.getSupportedActions()));

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
        Set<String> roots = new HashSet<>();
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
                    Iterables.any(cpRoots, t -> t.endsWith(entry)));
        }
    }

    private static void checkNotOnLookup(Lookup lookup, Class<?> type) {
        Collection<?> objects = lookup.lookupAll(type);
        if (!objects.isEmpty()) {
            fail("Lookup must not contain an instance of " + type.getName());
        }
    }

    private static void checkExactlyOnce(Lookup lookup, Class<?> type) {
        Collection<?> objects = lookup.lookupAll(type);
        if (objects.size() != 1) {
            fail("Lookup must contain exactly one entry of " + type.getName() + " instead of " + objects.size() + " times.");
        }
    }

    @Test
    public void testQueriesNotIncludedMultipleTimes() {
        Lookup lookup = rootProject.getLookup();

        checkExactlyOnce(lookup, LogicalViewProvider.class);
        checkExactlyOnce(lookup, ProjectInformation.class);
        checkExactlyOnce(lookup, ActionProvider.class);
        checkExactlyOnce(lookup, SharabilityQueryImplementation2.class);
        checkExactlyOnce(lookup, CustomizerProvider.class);
        checkExactlyOnce(lookup, ProjectConfigurationProvider.class);
        checkExactlyOnce(lookup, ProjectState.class);
        checkExactlyOnce(lookup, AuxiliaryConfiguration.class);
        checkExactlyOnce(lookup, AuxiliaryProperties.class);
        checkExactlyOnce(lookup, GradleCommandExecutor.class);
        checkExactlyOnce(lookup, GradleProperty.SourceEncoding.class);
        checkExactlyOnce(lookup, GradleProperty.ScriptPlatform.class);
        checkExactlyOnce(lookup, GradleProperty.SourceLevel.class);
        checkExactlyOnce(lookup, GradleProperty.BuildPlatform.class);
        checkExactlyOnce(lookup, ProjectIssueManager.class);
        checkExactlyOnce(lookup, Sources.class);
        checkExactlyOnce(lookup, GradleClassPathProvider.class);
        checkExactlyOnce(lookup, PrivilegedTemplates.class);
        checkExactlyOnce(lookup, RecommendedTemplates.class);
        checkExactlyOnce(lookup, NbGradleProject.class);
    }

    private static SourceGroup findGroupByName(String name, SourceGroup[] groups) {
        for (SourceGroup group: groups) {
            if (name.equals(group.getName())) {
                return group;
            }
        }
        return null;
    }

    @Test
    public void testSourcesMerger() {
        Lookup lookup = rootProject.getLookup();
        Sources sources = lookup.lookup(Sources.class);
        assertNotNull("sources", sources);

        SourceGroup[] groups = sources.getSourceGroups(JavaProjectConstants.SOURCES_TYPE_JAVA);
        SourceGroup foundGroup = findGroupByName(CustomSourcesMergerExtDef.TEST_SRC_GROUP_NAME, groups);
        assertNotNull("Must have the source groups merged.", foundGroup);
    }

    @Test
    public void testExtensionQueriesAreNotOnLookup() {
        Lookup lookup = rootProject.getLookup();

        checkNotOnLookup(lookup, JavaExtensionNodes.class);
        checkNotOnLookup(lookup, JavaProjectContextActions.class);
        checkNotOnLookup(lookup, GradleJavaBuiltInCommands.class);
    }

    @Test
    @SwingTest
    public void testHasProperNodes() throws Exception {
        LogicalViewProvider view = rootProject.getLookup().lookup(LogicalViewProvider.class);
        Node root = view.createLogicalView();

        Lookup children = Lookups.fixed((Object[])root.getChildren().getNodes());
        JavaDependenciesNode dependenciesNode = children.lookup(JavaDependenciesNode.class);
        BuildScriptsNode buildScriptsNode = children.lookup(BuildScriptsNode.class);

        assertNotNull("Must have a dependencies node", dependenciesNode);
        assertNotNull("Must have a build scripts node", buildScriptsNode);
    }

    private static void verifyJavaDocActionIsAdded(Action[] actions) {
        String searchedCaption = NbStrings.getJavadocCommandCaption();

        for (Action action: actions) {
            if (action == null) continue;

            Object name = action.getValue("Name");
            if (name == null) continue;

            if (searchedCaption.equals(name.toString())) {
                return;
            }
        }

        fail("Could not find javadoc command.");
    }

    @Test
    @SwingTest
    public void testJavadocActionIsAdded() throws Exception {
        LogicalViewProvider view = rootProject.getLookup().lookup(LogicalViewProvider.class);
        Node root = view.createLogicalView();

        verifyJavaDocActionIsAdded(root.getActions(false));
        verifyJavaDocActionIsAdded(root.getActions(true));
    }

    public static final class CustomSourcesMergerExtDef implements GradleProjectExtensionDef<Object> {
        private static final String TEST_SRC_GROUP_NAME = "CustomSourcesMergerExtDef.Name";

        @Override
        public String getName() {
            return CustomSourcesMergerExtDef.class.getName();
        }

        @Override
        public String getDisplayName() {
            return CustomSourcesMergerExtDef.class.getSimpleName();
        }

        @Override
        public Lookup getLookup() {
            return Lookup.EMPTY;
        }

        @Override
        public Class<Object> getModelType() {
            return Object.class;
        }

        @Override
        public ParsedModel<Object> parseModel(ModelLoadResult retrievedModels) {
            return new ParsedModel<>(new Object());
        }

        private static SourceGroup getTestSourceGroup() {
            return new SourceGroup() {

                @Override
                public FileObject getRootFolder() {
                    return FileUtil.getConfigRoot();
                }

                @Override
                public String getName() {
                    return TEST_SRC_GROUP_NAME;
                }

                @Override
                public String getDisplayName() {
                    return "";
                }

                @Override
                public Icon getIcon(boolean opened) {
                    return NbIcons.getGradleIconAsIcon();
                }

                @Override
                public boolean contains(FileObject file) {
                    return false;
                }

                @Override
                public void addPropertyChangeListener(PropertyChangeListener listener) {
                }

                @Override
                public void removePropertyChangeListener(PropertyChangeListener listener) {
                }
            };
        }

        private static Sources getTestSources() {
            return new Sources() {
                @Override
                public SourceGroup[] getSourceGroups(String type) {
                    if (JavaProjectConstants.SOURCES_TYPE_JAVA.equals(type)) {
                        return new SourceGroup[]{getTestSourceGroup()};
                    }
                    else {
                        return new SourceGroup[0];
                    }
                }

                @Override
                public void addChangeListener(ChangeListener listener) {
                }

                @Override
                public void removeChangeListener(ChangeListener listener) {
                }
            };
        }

        @Override
        public GradleProjectExtension2<Object> createExtension(Project project) throws IOException {
            return new GradleProjectExtension2<Object>() {
                @Override
                public Lookup getPermanentProjectLookup() {
                    return Lookup.EMPTY;
                }

                @Override
                public Lookup getProjectLookup() {
                    return Lookups.singleton(getTestSources());
                }

                @Override
                public Lookup getExtensionLookup() {
                    return Lookup.EMPTY;
                }

                @Override
                public void activateExtension(Object parsedModel) {
                }

                @Override
                public void deactivateExtension() {
                }
            };
        }

        @Override
        public Set<String> getSuppressedExtensions() {
            return Collections.emptySet();
        }

    }
}