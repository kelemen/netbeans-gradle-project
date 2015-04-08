package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.io.File;
import java.util.List;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public final class GradleHomeNode extends AbstractNode {
    public GradleHomeNode() {
        super(createChildren());
    }

    private static Children createChildren() {
        return Children.create(new GradleHomeNodeChildFactory(), true);
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
        // TODO: I18N
        return "Gradle Home";
    }

    private static class GradleHomeNodeChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory> {
        private final ListenerRegistrations listenerRefs;

        public GradleHomeNodeChildFactory() {
            this.listenerRefs = new ListenerRegistrations();
        }

        @Override
        protected void addNotify() {
            listenerRefs.add(GradleFileUtils.GRADLE_USER_HOME.addChangeListener(new Runnable() {
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

        private static File getUserHome() {
            return GradleFileUtils.GRADLE_USER_HOME.getValue();
        }

        private static FileObject tryGetFile(File dir, String name) {
            return FileUtil.toFileObject(new File(dir, name));
        }

        private void addGradleProperties(File userHome, List<SingleNodeFactory> toPopulate) {
            FileObject gradleProperties = tryGetFile(userHome, SettingsFiles.GRADLE_PROPERTIES_NAME);
            if (gradleProperties != null) {
                SingleNodeFactory node = NodeUtils.tryGetFileNode(gradleProperties);
                if (node != null) {
                    toPopulate.add(node);
                }
            }
        }

        private void addInitGradle(File userHome, List<SingleNodeFactory> toPopulate) {
            FileObject initGradle = tryGetFile(userHome, "init.gradle");
            if (initGradle != null) {
                SingleNodeFactory node = NodeUtils.tryGetFileNode(
                        initGradle,
                        initGradle.getNameExt(),
                        NbIcons.getGradleIcon());
                if (node != null) {
                    toPopulate.add(node);
                }
            }
        }

        private void addInitDDir(File userHome, List<SingleNodeFactory> toPopulate) {
            final FileObject initD = tryGetFile(userHome, "init.d");

            if (initD != null && initD.isFolder()) {
                // TODO: I18N
                toPopulate.add(GradleFolderNode.getFactory("Global init scripts", initD));
            }
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            File userHome = getUserHome();
            if (userHome == null) {
                return;
            }

            addGradleProperties(userHome, toPopulate);
            addInitGradle(userHome, toPopulate);
            addInitDDir(userHome, toPopulate);
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
}
