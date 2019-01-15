package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.AsyncTasks;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.util.RefreshableChildren;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class BuildScriptsNode extends AbstractNode {
    private final NbGradleProject project;
    private final BuildScriptChildFactory childFactory;

    public BuildScriptsNode(NbGradleProject project) {
        this(project, new BuildScriptChildFactory(project));
    }

    private BuildScriptsNode(NbGradleProject project, BuildScriptChildFactory childFactory) {
        this(project, childFactory, Children.create(childFactory, true));
    }

    private BuildScriptsNode(
            NbGradleProject project,
            BuildScriptChildFactory childFactory,
            Children children) {
        super(children, createLookup(childFactory, children));

        this.project = project;
        this.childFactory = childFactory;

        setName(getClass().getSimpleName());
    }

    private static Lookup createLookup(BuildScriptChildFactory childFactory, Children children) {
        return Lookups.fixed(
                NodeUtils.askChildrenNodeFinder(),
                NodeUtils.defaultNodeRefresher(children, childFactory));
    }

    public static SingleNodeFactory getFactory(NbGradleProject project) {
        return new NodeFactoryImpl(project);
    }

    private static void addOpenFileAction(Path file, List<Action> actions) {
        if (file != null) {
            actions.add(new OpenAlwaysFileAction(file));
        }
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>();

        NbGradleModel currentModel = project.currentModel().getValue();
        addOpenFileAction(currentModel.getSettingsFile(), actions);
        addOpenFileAction(currentModel.getBuildFile().toPath(), actions);
        actions.add(null);
        actions.add(new OpenOrCreateBuildSrc(
                NbStrings.getOpenBuildSrcCaption(),
                project,
                childFactory));

        if (!currentModel.isRootProject()) {
            actions.add(OpenProjectsAction.createFromProjectDirs(
                    NbStrings.getOpenRootProjectsCaption(),
                    Collections.singleton(currentModel.getSettingsDir().toFile())));
        }

        actions.add(null);
        actions.add(NodeUtils.getRefreshNodeAction(this));

        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public Image getIcon(int type) {
        return NbIcons.getFolderIcon();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return NbIcons.getOpenFolderIcon();
    }

    @Override
    public String getDisplayName() {
        return NbStrings.getBuildScriptsNodeCaption();
    }

    private static Path getBuildSrcDir(NbGradleProject project) {
         return getBuildSrcDir(project.currentModel().getValue());
    }

    private static Path getBuildSrcDir(NbGradleModel currentModel) {
        return currentModel.getSettingsDir().resolve(CommonScripts.BUILD_SRC_NAME);
    }

    private static class BuildScriptChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory>
    implements
            RefreshableChildren {

        private final NbGradleProject project;
        private final ListenerRegistrations listenerRefs;
        private volatile boolean createdOnce;

        public BuildScriptChildFactory(NbGradleProject project) {
            this.project = Objects.requireNonNull(project, "project");
            this.listenerRefs = new ListenerRegistrations();
            this.createdOnce = false;
        }

        @Override
        public void refreshChildren() {
            if (createdOnce) {
                refresh(false);
            }
        }

        @Override
        protected void addNotify() {
            listenerRefs.add(project.currentModel().addChangeListener(this::refreshChildren));
        }

        @Override
        protected void removeNotify() {
            listenerRefs.unregisterAll();
        }

        private static void addProjectScriptsNode(
                String caption,
                File projectDir,
                List<SingleNodeFactory> toPopulate) {
            NbGradleProject project = NbGradleProjectFactory.tryLoadSafeGradleProject(projectDir);
            if (project != null) {
                toPopulate.add(ProjectScriptFilesNode.getFactory(caption, project));
            }
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            NbGradleModel currentModel = project.currentModel().getValue();

            if (!currentModel.isBuildSrc()) {
                Path buildSrc = getBuildSrcDir(currentModel);
                if (Files.isDirectory(buildSrc)) {
                    toPopulate.add(new BuildSrcNodeFactory(buildSrc));
                }
            }

            File projectDir = currentModel.getProjectDir();
            addProjectScriptsNode(NbStrings.getProjectScriptNodeCaption(), projectDir, toPopulate);

            File rootProjectDir = currentModel.getSettingsDir().toFile();
            if (!Objects.equals(projectDir, rootProjectDir)) {
                addProjectScriptsNode(NbStrings.getRootProjectScriptNodeCaption(), rootProjectDir, toPopulate);
            }

            toPopulate.add(GradleHomeNode.getFactory(project.getScriptFileProvider()));
        }

        @Override
        protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
            createdOnce = true;
            readKeys(toPopulate);
            return true;
        }

        @Override
        protected Node createNodeForKey(SingleNodeFactory key) {
            return key.createNode();
        }
    }

    @SuppressWarnings("serial")
    private static class OpenOrCreateBuildSrc extends AbstractAction {
        private final NbGradleProject project;
        private final BuildScriptChildFactory childFactory;

        public OpenOrCreateBuildSrc(
                String name,
                NbGradleProject project,
                BuildScriptChildFactory childFactory) {

            super(name);
            this.project = project;
            this.childFactory = childFactory;
        }

        private void openProjectNow(Path buildSrcDir) {
            OpenProjectsAction.openProject(buildSrcDir);
        }

        private static Path resolveAndCreate(Path parent, String name) throws IOException {
            Path result = parent.resolve(name);
            Files.createDirectories(result);
            return result;
        }

        private void createDirs(Path buildSrcDir) throws IOException {
            Files.createDirectories(buildSrcDir);

            Path srcDir = resolveAndCreate(buildSrcDir, "src");

            Path srcMainDir = resolveAndCreate(srcDir, "main");
            resolveAndCreate(srcMainDir, "groovy");
            resolveAndCreate(srcMainDir, "resources");

            Path srcTestDir = resolveAndCreate(srcDir, "test");
            resolveAndCreate(srcTestDir, "groovy");
            resolveAndCreate(srcTestDir, "resources");
        }

        private void createBuildSrc(Path buildSrcDir) throws IOException {
            createDirs(buildSrcDir);

            Path buildGradle = buildSrcDir.resolve(CommonScripts.BUILD_BASE_NAME + CommonScripts.DEFAULT_SCRIPT_EXTENSION);
            List<String> buildGradleContent = Arrays.asList(
                    "apply plugin: 'groovy'",
                    "",
                    "dependencies {",
                    "    compile gradleApi()",
                    "    compile localGroovy()",
                    "}");
            NbFileUtils.writeLinesToFile(buildGradle, buildGradleContent, StringUtils.UTF8, project);

            childFactory.refreshChildren();
        }

        private void confirmAndCreateProject(final Path buildSrcDir) {
            String message = NbStrings.getConfirmCreateBuildSrcMessage();
            String title = NbStrings.getConfirmCreateBuildSrcTitle();
            NotifyDescriptor d = new NotifyDescriptor.Confirmation(message, title, NotifyDescriptor.YES_NO_OPTION, NotifyDescriptor.QUESTION_MESSAGE);
            if (DialogDisplayer.getDefault().notify(d) == NotifyDescriptor.YES_OPTION) {
                NbTaskExecutors.DEFAULT_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, (cancelToken) -> {
                    createBuildSrc(buildSrcDir);
                    openProjectNow(buildSrcDir);
                }).exceptionally(AsyncTasks::expectNoError);
            }
        }

        private void doActionNow(final Path buildSrcDir) {
            if (Files.isDirectory(buildSrcDir)) {
                openProjectNow(buildSrcDir);
            }
            else {
                SwingUtilities.invokeLater(() -> {
                    confirmAndCreateProject(buildSrcDir);
                });
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NbTaskExecutors.DEFAULT_EXECUTOR.execute(() -> doActionNow(getBuildSrcDir(project)));
        }
    }

    private static class BuildSrcNodeFactory implements SingleNodeFactory {
        private final Path buildSrcDir;

        public BuildSrcNodeFactory(Path buildSrcDir) {
            this.buildSrcDir = buildSrcDir;
        }

        @Override
        public Node createNode() {
            return new BuildSrcNode(buildSrcDir);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 73 * hash + Objects.hashCode(this.buildSrcDir);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final BuildSrcNodeFactory other = (BuildSrcNodeFactory)obj;
            return Objects.equals(this.buildSrcDir, other.buildSrcDir);
        }
    }

    private static class BuildSrcNode extends FilterNode {
        private final Path buildSrcDir;

        public BuildSrcNode(Path buildSrcDir) {
            super(Node.EMPTY.cloneNode(), null, Lookups.fixed(buildSrcDir));
            this.buildSrcDir = buildSrcDir;
        }

        @Override
        public Action[] getActions(boolean context) {
            return new Action[]{
                getPreferredAction()
            };
        }

        @Override
        public Action getPreferredAction() {
            return OpenProjectsAction.createFromProjectDirs(
                    NbStrings.getOpenBuildSrcCaption(),
                    Collections.singleton(buildSrcDir.toFile()));
        }

        @Override
        public String getName() {
            Path parentFile = buildSrcDir.getParent();
            return "BuildSrc_" + (parentFile != null ? NbFileUtils.getFileNameStr(parentFile) : "unknown");
        }
        @Override
        public String getDisplayName() {
            return NbStrings.getBuildSrcNodeCaption();
        }

        @Override
        public Image getIcon(int type) {
            return NbIcons.getGradleIcon();
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public boolean canRename() {
            return false;
        }
    }

    private static class NodeFactoryImpl implements SingleNodeFactory {
        private final NbGradleProject project;
        private final File projectDir;

        public NodeFactoryImpl(NbGradleProject project) {
            this.project = Objects.requireNonNull(project, "project");
            this.projectDir = project.getProjectDirectoryAsFile();
        }

        @Override
        public Node createNode() {
            return new BuildScriptsNode(project);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 23 * hash + Objects.hashCode(this.projectDir);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final NodeFactoryImpl other = (NodeFactoryImpl)obj;
            return Objects.equals(this.projectDir, other.projectDir);
        }
    }
}
