package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.Action;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.query.GradleFilesClassPathProvider;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
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

        private void addFileObject(
                FileObject file,
                List<SingleNodeFactory> toPopulate) {
            SingleNodeFactory nodeFactory = NodeUtils.tryGetFileNode(file);
            if (nodeFactory != null) {
                toPopulate.add(nodeFactory);
            }
        }

        private void addGradleFile(
                FileObject file,
                List<SingleNodeFactory> toPopulate) {
            addGradleFile(file, file.getNameExt(), toPopulate);
        }

        private void addGradleFile(
                FileObject file,
                final String name,
                List<SingleNodeFactory> toPopulate) {

            SingleNodeFactory nodeFactory = NodeUtils.tryGetFileNode(file, name, NbIcons.getGradleIcon());
            if (nodeFactory != null) {
                toPopulate.add(nodeFactory);
            }
        }

        private static File getLocalGradleProperties(NbGradleModel model) {
            return new File(model.getProjectDir(), SettingsFiles.GRADLE_PROPERTIES_NAME);
        }

        private static FileObject tryGetLocalGradlePropertiesObj(NbGradleModel model) {
            File result = getLocalGradleProperties(model);
            return FileUtil.toFileObject(result);
        }

        private FileObject getRootBuildDir(FileObject settingsGradle) {
            if (settingsGradle != null) {
                return settingsGradle.getParent();
            }
            else {
                return project.getProjectDirectory();
            }
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            NbGradleModel model = project.currentModel().getValue();

            FileObject settingsGradle = model.tryGetSettingsFileObj();
            FileObject rootBuildDir = getRootBuildDir(settingsGradle);

            if (rootBuildDir != null && !model.isBuildSrc()) {
                FileObject buildSrcObj = rootBuildDir.getFileObject(SettingsFiles.BUILD_SRC_NAME);
                final File buildSrc = buildSrcObj != null
                        ? FileUtil.toFile(buildSrcObj)
                        : null;
                if (buildSrc != null) {
                    toPopulate.add(new SingleNodeFactory() {
                        @Override
                        public Node createNode() {
                            return new BuildSrcNode(buildSrc);
                        }
                    });
                }
            }

            if (settingsGradle != null) {
                addGradleFile(settingsGradle, toPopulate);
            }

            FileObject buildGradle = model.tryGetBuildFileObj();
            if (buildGradle != null) {
                addGradleFile(buildGradle, toPopulate);
            }

            FileObject propertiesFile = tryGetLocalGradlePropertiesObj(model);
            if (propertiesFile != null) {
                addFileObject(propertiesFile, toPopulate);
            }

            List<FileObject> gradleFiles = new LinkedList<>();
            for (FileObject file : project.getProjectDirectory().getChildren()) {
                if (file.equals(buildGradle) || file.equals(settingsGradle)) {
                    continue;
                }

                if (GradleFilesClassPathProvider.isGradleFile(file)) {
                    gradleFiles.add(file);
                }
            }

            Collections.sort(gradleFiles, new Comparator<FileObject>() {
                @Override
                public int compare(FileObject o1, FileObject o2) {
                    return StringUtils.STR_CMP.compare(o1.getNameExt(), o2.getNameExt());
                }
            });

            for (FileObject file: gradleFiles) {
                addGradleFile(file, toPopulate);
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
}
