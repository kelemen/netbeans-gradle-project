package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.text.Collator;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.GradleProjectConstants;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.query.GradleFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

public final class BuildScriptsNode extends AbstractNode {
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
            if (project == null)
                throw new NullPointerException("project");
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
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            addFileObject(file, file.getNameExt(), toPopulate);
        }

        private void addFileObject(
                FileObject file,
                final String name,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            final DataObject fileData = DataObject.find(file);

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
                    };
                }
            });
        }

        private void addGradleFile(
                FileObject file,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            addGradleFile(file, file.getNameExt(), toPopulate);
        }

        private void addGradleFile(
                FileObject file,
                final String name,
                List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            final DataObject fileData = DataObject.find(file);

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

        private static FileObject tryGetLocalGradleProperties(NbGradleModel model) {
            return model.getProjectDir().getFileObject(GradleProjectConstants.GRADLE_PROPERTIES_NAME);
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) throws DataObjectNotFoundException {
            NbGradleModel model = project.getCurrentModel();

            FileObject settingsGradle = model.getSettingsFile();
            if (settingsGradle != null) {
                addGradleFile(settingsGradle, toPopulate);
            }

            FileObject buildGradle = model.getBuildFile();
            if (buildGradle != null) {
                addGradleFile(buildGradle, toPopulate);
            }

            FileObject homePropertiesFile = tryGetHomeGradleProperties();
            if (homePropertiesFile != null) {
                addFileObject(homePropertiesFile,
                        NbStrings.getUserHomeFileName(GradleProjectConstants.GRADLE_PROPERTIES_NAME),
                        toPopulate);
            }

            FileObject propertiesFile = tryGetLocalGradleProperties(model);
            if (propertiesFile != null) {
                addFileObject(propertiesFile, toPopulate);
            }

            List<FileObject> gradleFiles = new LinkedList<FileObject>();
            for (FileObject file : project.getProjectDirectory().getChildren()) {
                if (file.equals(buildGradle) || file.equals(settingsGradle)) {
                    continue;
                }

                if ("gradle".equalsIgnoreCase(file.getExt())) {
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
            try {
                readKeys(toPopulate);
            } catch (DataObjectNotFoundException ex) {
                throw new RuntimeException(ex);
            }
            return true;
        }

        @Override
        protected Node createNodeForKey(SingleNodeFactory key) {
            return key.createNode();
        }
    }
}
