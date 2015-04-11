package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.util.List;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jtrim.event.EventListeners;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ProxyListenerRegistry;
import org.jtrim.event.SimpleListenerRegistry;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.event.NbListenerManagers;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public final class GradleHomeNode extends AbstractNode {
    private static final String INIT_GRADLE_NAME = "init.gradle";

    private final GradleHomeNodeChildFactory childFactory;

    public GradleHomeNode() {
        this(new GradleHomeNodeChildFactory());
    }

    private GradleHomeNode(GradleHomeNodeChildFactory childFactory) {
        super(Children.create(childFactory, true));

        this.childFactory = childFactory;
    }

    public static SingleNodeFactory getFactory() {
        return FactoryImpl.INSTANCE;
    }

    private Action openGradleHomeFile(String name) {
        File userHome = getUserHome();
        File file = userHome != null ? new File(userHome, name) : new File(name);

        String caption = NbStrings.getOpenFileCaption(name);
        Action result = new OpenAlwaysFileAction(caption, file.toPath());
        if (userHome == null) {
            result.setEnabled(false);
        }

        return result;
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            openGradleHomeFile(INIT_GRADLE_NAME),
            openGradleHomeFile(SettingsFiles.GRADLE_PROPERTIES_NAME),
            null,
            new RefreshNodesAction()
        };
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
        return NbStrings.getGradleHomeNodeCaption();
    }

    private static File getUserHome() {
        return GradleFileUtils.GRADLE_USER_HOME.getValue();
    }

    @SuppressWarnings("serial") // don't care about serialization
    private class RefreshNodesAction extends AbstractAction {
        public RefreshNodesAction() {
            super(NbStrings.getScanForChangesCaption());
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
        private final ProxyListenerRegistry<Runnable> userHomeChangeListeners;

        public GradleHomeNodeChildFactory() {
            this.listenerRefs = new ListenerRegistrations();
            this.userHomeChangeListeners = new ProxyListenerRegistry<>(NbListenerManagers.neverNotifingRegistry());
        }

        public void refreshChildren() {
            refresh(false);
        }

        private FileObject tryGetUserHomeObj() {
            File userHome = GradleFileUtils.GRADLE_USER_HOME.getValue();
            if (userHome == null) {
                return null;
            }

            return FileUtil.toFileObject(userHome);
        }

        private void updateUserHome() {
            final FileObject userHome = tryGetUserHomeObj();
            if (userHome == null) {
                userHomeChangeListeners.replaceRegistry(NbListenerManagers.neverNotifingRegistry());
            }
            else {
                userHomeChangeListeners.replaceRegistry(new SimpleListenerRegistry<Runnable>() {
                    @Override
                    public ListenerRef registerListener(Runnable listener) {
                        return NbFileUtils.addDirectoryContentListener(userHome, listener);
                    }
                });
            }
            userHomeChangeListeners.onEvent(EventListeners.runnableDispatcher(), null);
        }

        @Override
        protected void addNotify() {
            listenerRefs.add(GradleFileUtils.GRADLE_USER_HOME.addChangeListener(new Runnable() {
                @Override
                public void run() {
                    updateUserHome();
                }
            }));
            updateUserHome();
            listenerRefs.add(userHomeChangeListeners.registerListener(new Runnable() {
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
            FileObject initGradle = tryGetFile(userHome, INIT_GRADLE_NAME);
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
                toPopulate.add(GradleFolderNode.getFactory(
                        NbStrings.getGlobalInitScriptsNodeCaption(),
                        initD));
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
