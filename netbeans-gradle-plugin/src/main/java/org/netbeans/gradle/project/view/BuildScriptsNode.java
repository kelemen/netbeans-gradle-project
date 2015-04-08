package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import javax.swing.Action;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
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
    public BuildScriptsNode(NbGradleProject project) {
        super(createChildren(project));
    }

    private static Children createChildren(NbGradleProject project) {
        return Children.create(new BuildScriptChildFactory(project), true);
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
        private volatile ChildrenCreateInfo lastShownInfo;

        public BuildScriptChildFactory(NbGradleProject project) {
            ExceptionHelper.checkNotNullArgument(project, "project");
            this.project = project;
            this.listenerRefs = new ListenerRegistrations();
            this.lastShownInfo = null;
        }

        private void refreshIfNeeded() {
            NbGradleModel model = project.currentModel().getValue();
            ChildrenCreateInfo createInfo = new ChildrenCreateInfo(model);
            if (!Objects.equals(createInfo, lastShownInfo)) {
                refresh(false);
            }
        }

        @Override
        protected void addNotify() {
            listenerRefs.add(project.currentModel().addChangeListener(new Runnable() {
                @Override
                public void run() {
                    refreshIfNeeded();
                }
            }));
        }

        @Override
        protected void removeNotify() {
            listenerRefs.unregisterAll();
        }

        private void addProjectScriptsNode(
                final String caption,
                final File projectDir,
                List<SingleNodeFactory> toPopulate) {
            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return new ProjectScriptFilesNode(caption, projectDir);
                }
            });
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            ChildrenCreateInfo createInfo = new ChildrenCreateInfo(project.currentModel().getValue());
            lastShownInfo = createInfo;

            File rootProjectDir = createInfo.rootProjectDir;

            if (!createInfo.buildSrc) {
                final File buildSrc = new File(rootProjectDir, SettingsFiles.BUILD_SRC_NAME);
                if (buildSrc.isDirectory()) {
                    toPopulate.add(new SingleNodeFactory() {
                        @Override
                        public Node createNode() {
                            return new BuildSrcNode(buildSrc);
                        }
                    });
                }
            }

            File projectDir = createInfo.projectDir;
            // TODO: I18N
            addProjectScriptsNode("Project", projectDir, toPopulate);

            if (!Objects.equals(projectDir, rootProjectDir)) {
                // TODO: I18N
                addProjectScriptsNode("Root Project", rootProjectDir, toPopulate);
            }

            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return new GradleHomeNode();
                }
            });
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

    private static final class ChildrenCreateInfo {
        public final File rootProjectDir;
        public final File projectDir;
        public final boolean buildSrc;

        public ChildrenCreateInfo(NbGradleModel model) {
            this.rootProjectDir = model.getRootProjectDir();
            this.projectDir = model.getProjectDir();
            this.buildSrc = model.isBuildSrc();
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 67 * hash + Objects.hashCode(this.rootProjectDir);
            hash = 67 * hash + Objects.hashCode(this.projectDir);
            hash = 67 * hash + (this.buildSrc ? 1 : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final ChildrenCreateInfo other = (ChildrenCreateInfo)obj;
            return Objects.equals(this.rootProjectDir, other.rootProjectDir)
                    && Objects.equals(this.projectDir, other.projectDir)
                    && this.buildSrc == other.buildSrc;
        }
    }
}
