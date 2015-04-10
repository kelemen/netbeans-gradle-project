package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.Action;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;

public final class BuildScriptsNode extends AbstractNode {
    private final NbGradleProject project;

    public BuildScriptsNode(NbGradleProject project) {
        super(createChildren(project));

        this.project = project;
    }

    public static SingleNodeFactory getFactory(final NbGradleProject project) {
        return new NodeFactoryImpl(project);
    }

    private static Children createChildren(NbGradleProject project) {
        return Children.create(new BuildScriptChildFactory(project), true);
    }

    private static Action openFileAction(File file) {
        String actionCaption = NbStrings.getOpenFileCaption(file.getName());
        Action result = new OpenAlwaysFileAction(actionCaption, file.toPath());

        return result;
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> actions = new ArrayList<>(2);

        NbGradleModel currentModel = project.currentModel().getValue();
        actions.add(openFileAction(currentModel.getSettingsFile()));
        actions.add(openFileAction(currentModel.getBuildFile()));

        return actions.toArray(new Action[actions.size()]);
    }

    @Override
    public Image getIcon(int type) {
        return NbIcons.getFolderIcon();
    }

    @Override
    public Image getOpenedIcon(int type) {
        return getIcon(type);
    }

    @Override
    public String getDisplayName() {
        return NbStrings.getBuildScriptsNodeCaption();
    }

    private static class BuildScriptChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory> {
        private final NbGradleProject project;
        private final ListenerRegistrations listenerRefs;

        public BuildScriptChildFactory(NbGradleProject project) {
            ExceptionHelper.checkNotNullArgument(project, "project");
            this.project = project;
            this.listenerRefs = new ListenerRegistrations();
        }

        @Override
        protected void addNotify() {
            listenerRefs.add(project.currentModel().addChangeListener(new Runnable() {
                @Override
                public void run() {
                    refresh(false);
                }
            }));
        }

        @Override
        protected void removeNotify() {
            listenerRefs.unregisterAll();
        }

        private static void addProjectScriptsNode(
                String caption,
                File projectDir,
                List<SingleNodeFactory> toPopulate) {
            Project project = NbGradleProjectFactory.tryLoadSafeProject(projectDir);
            if (project != null) {
                NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
                if (gradleProject != null) {
                    toPopulate.add(ProjectScriptFilesNode.getFactory(caption, gradleProject));
                }
            }
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            NbGradleModel currentModel = project.currentModel().getValue();

            File rootProjectDir = currentModel.getRootProjectDir();

            if (!currentModel.isBuildSrc()) {
                final File buildSrc = new File(rootProjectDir, SettingsFiles.BUILD_SRC_NAME);
                if (buildSrc.isDirectory()) {
                    toPopulate.add(new BuildSrcNodeFactory(buildSrc));
                }
            }

            File projectDir = currentModel.getProjectDir();
            addProjectScriptsNode(NbStrings.getProjectScriptNodeCaption(), projectDir, toPopulate);

            if (!Objects.equals(projectDir, rootProjectDir)) {
                addProjectScriptsNode(NbStrings.getRootProjectScriptNodeCaption(), rootProjectDir, toPopulate);
            }

            toPopulate.add(GradleHomeNode.getFactory());
        }

        @Override
        protected boolean createKeys(List<SingleNodeFactory> toPopulate) {
            readKeys(toPopulate);
            return true;
        }

        @Override
        protected Node createNodeForKey(SingleNodeFactory key) {
            return key.createNode();
        }
    }

    private static class BuildSrcNodeFactory implements SingleNodeFactory {
        private final File buildSrcDir;

        public BuildSrcNodeFactory(File buildSrcDir) {
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
        private final File buildSrcDir;

        public BuildSrcNode(File buildSrcDir) {
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
                    Collections.singleton(buildSrcDir));
        }

        @Override
        public String getName() {
            File parentFile = buildSrcDir.getParentFile();
            return "BuildSrc_" + (parentFile != null ? parentFile.getName() : "unknown");
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
            this.project = project;
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
