package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
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
    private final GradleHomeNodeChildFactory childFactory;
    private final Action[] contextActions;

    public GradleHomeNode() {
        this(new GradleHomeNodeChildFactory());
    }

    private GradleHomeNode(GradleHomeNodeChildFactory childFactory) {
        super(Children.create(childFactory, true));

        this.childFactory = childFactory;
        this.contextActions = new Action[] {
            new RefreshNodesAction()
        };
    }

    public SingleNodeFactory getFactory() {
        return FactoryImpl.INSTANCE;
    }

    @Override
    public Action[] getActions(boolean context) {
        return contextActions.clone();
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

    @SuppressWarnings("serial") // don't care about serialization
    private class RefreshNodesAction extends AbstractAction {
        public RefreshNodesAction() {
            // TODO: I18N
            super("Scan for changes");
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            childFactory.refreshChildren();
        }
    }

    private static class GradleHomeNodeChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory> {
        private final ListenerRegistrations listenerRefs;

        public GradleHomeNodeChildFactory() {
            this.listenerRefs = new ListenerRegistrations();
        }

        public void refreshChildren() {
            refresh(false);
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

    private enum FactoryImpl implements SingleNodeFactory {
        INSTANCE;

        @Override
        public Node createNode() {
            return new GradleHomeNode();
        }
    }
}
