package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.io.File;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.netbeans.gradle.project.query.GradleFilesClassPathProvider;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;

public final class BuildScriptsNode extends AbstractNode {
    private static final Logger LOGGER = Logger.getLogger(BuildScriptsNode.class.getName());
    private static final Collator STR_CMP = Collator.getInstance();

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
            ChildFactory.Detachable<SingleNodeFactory>
    implements
            ChangeListener {
        private final NbGradleProject project;

        public BuildScriptChildFactory(NbGradleProject project) {
            ExceptionHelper.checkNotNullArgument(project, "project");
            this.project = project;
        }

        @Override
        public void stateChanged(ChangeEvent e) {
            refresh(false);
        }

        @Override
        protected void addNotify() {
            project.addModelChangeListener(this);
        }

        @Override
        protected void removeNotify() {
            project.removeModelChangeListener(this);
        }

        private void addFileObject(
                FileObject file,
                List<SingleNodeFactory> toPopulate) {
            addFileObject(file, file.getNameExt(), toPopulate);
        }

        private static DataObject tryGetDataObject(FileObject fileObj) {
            try {
                return DataObject.find(fileObj);
            } catch (DataObjectNotFoundException ex) {
                LOGGER.log(Level.INFO, "Failed to find DataObject for file object: " + fileObj.getPath(), ex);
                return null;
            }
        }

        private void addFileObject(
                FileObject file,
                final String name,
                List<SingleNodeFactory> toPopulate) {
            final DataObject fileData = tryGetDataObject(file);
            if (fileData == null) {
                return;
            }

            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return new FilterNode(fileData.getNodeDelegate().cloneNode()) {
                        @Override
                        public boolean canRename() {
                            return false;
                        }

                        @Override
                        public String getDisplayName() {
                            return name;
                        }
                    };
                }
            });
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
            final DataObject fileData = tryGetDataObject(file);
            if (fileData == null) {
                return;
            }

            toPopulate.add(new SingleNodeFactory() {
                @Override
                public Node createNode() {
                    return new FilterNode(fileData.getNodeDelegate()) {
                        @Override
                        public boolean canRename() {
                            return false;
                        }

                        @Override
                        public String getDisplayName() {
                            return name;
                        }

                        @Override
                        public Image getIcon(int type) {
                            return NbIcons.getGradleIcon();
                        }

                        @Override
                        public Image getOpenedIcon(int type) {
                            return getIcon(type);
                        }
                    };
                }
            });
        }

        private static FileObject tryGetHomeGradleProperties() {
            FileObject userHome = GradleFileUtils.getGradleUserHomeFileObject();
            return userHome != null
                    ? userHome.getFileObject(GradleProjectConstants.GRADLE_PROPERTIES_NAME)
                    : null;
        }

        private static File getLocalGradleProperties(NbGradleModel model) {
            return new File(model.getProjectDir(), GradleProjectConstants.GRADLE_PROPERTIES_NAME);
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
            NbGradleModel model = project.getCurrentModel();

            FileObject settingsGradle = model.tryGetSettingsFileObj();
            FileObject rootBuildDir = getRootBuildDir(settingsGradle);

            if (rootBuildDir != null && !model.isBuildSrc()) {
                FileObject buildSrcObj = rootBuildDir.getFileObject(GradleProjectConstants.BUILD_SRC_NAME);
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

            FileObject homePropertiesFile = tryGetHomeGradleProperties();
            if (homePropertiesFile != null) {
                addFileObject(homePropertiesFile,
                        NbStrings.getUserHomeFileName(GradleProjectConstants.GRADLE_PROPERTIES_NAME),
                        toPopulate);
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
                    return STR_CMP.compare(o1.getNameExt(), o2.getNameExt());
                }
            });

            for (FileObject file : gradleFiles) {
                addGradleFile(file, toPopulate);
            }
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
