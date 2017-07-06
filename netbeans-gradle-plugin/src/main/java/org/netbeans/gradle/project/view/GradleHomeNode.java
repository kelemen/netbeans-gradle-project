package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.event.EventListeners;
import org.jtrim2.event.ProxyListenerRegistry;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.event.NbListenerManagers;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.util.RefreshableChildren;
import org.netbeans.gradle.project.util.StringUtils;
import org.netbeans.spi.project.ui.PathFinder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class GradleHomeNode extends AbstractNode {
    // Though as of Gradle 3.2.1, Kotling init scripts are not supported, we will assume support
    // because it is reasonable to expect support for them in the future.

    private static final String INIT_GRADLE_BASE_NAME = "init";
    private static final String INIT_D_NAME = "init.d";

    private final GradleHomeNodeChildFactory childFactory;
    private final ScriptFileProvider scriptProvider;

    public GradleHomeNode(ScriptFileProvider scriptProvider) {
        this(new GradleHomeNodeChildFactory(scriptProvider), scriptProvider);
    }

    private GradleHomeNode(GradleHomeNodeChildFactory childFactory, ScriptFileProvider scriptProvider) {
        this(childFactory, scriptProvider, Children.create(childFactory, true));
    }

    private GradleHomeNode(GradleHomeNodeChildFactory childFactory, ScriptFileProvider scriptProvider, Children children) {
        super(children, createLookup(childFactory, scriptProvider, children));

        this.childFactory = childFactory;
        this.scriptProvider = scriptProvider;

        setName(getClass().getSimpleName());
    }

    private static Lookup createLookup(
            GradleHomeNodeChildFactory childFactory,
            ScriptFileProvider scriptProvider,
            Children children) {
        return Lookups.fixed(
                new GradleHomePathFinder(scriptProvider),
                NodeUtils.defaultNodeRefresher(children, childFactory));
    }

    public static SingleNodeFactory getFactory(ScriptFileProvider scriptProvider) {
        return new FactoryImpl(scriptProvider);
    }

    private Action openGradleHomeFile(String name) {
        Path userHome = getGradleUserHome();
        Path file = userHome != null ? userHome.resolve(name) : Paths.get(name);

        Action result = new OpenAlwaysFileAction(file);
        if (userHome == null) {
            result.setEnabled(false);
        }

        return result;
    }

    private Action openGradleHomeScriptFile(String baseName) {
        Path userHome = getGradleUserHome();
        Path baseDir = userHome != null ? userHome : Paths.get(".");

        Action result = OpenAlwaysFileAction.openScriptAction(baseDir, baseName, scriptProvider);
        if (userHome == null) {
            result.setEnabled(false);
        }

        return result;
    }

    @Override
    public Action[] getActions(boolean context) {
        List<Action> result = new ArrayList<>();

        result.add(openGradleHomeScriptFile(INIT_GRADLE_BASE_NAME));
        result.add(openGradleHomeFile(CommonScripts.GRADLE_PROPERTIES_NAME));
        if (!childFactory.hasInitDDirDisplayed) {
            result.add(new CreateInitDAction());
        }
        result.add(null);
        result.add(NodeUtils.getRefreshNodeAction(this));

        return result.toArray(new Action[result.size()]);
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
        return NbStrings.getGradleHomeNodeCaption();
    }

    private static File getGradleUserHomeFile() {
        return GradleFileUtils.GRADLE_USER_HOME.getValue();
    }

    private static Path getGradleUserHome() {
        File result = getGradleUserHomeFile();
        return result != null ? result.toPath() : null;
    }

    private static final class GradleHomePathFinder implements PathFinder {
        private final ScriptFileProvider scriptProvider;

        public GradleHomePathFinder(ScriptFileProvider scriptProvider) {
            this.scriptProvider = Objects.requireNonNull(scriptProvider, "scriptProvider");
        }

        @Override
        public Node findPath(Node root, Object target) {
            FileObject targetFile = NodeUtils.tryGetFileSearchTarget(target);
            if (targetFile == null) {
                return null;
            }

            String baseName = targetFile.getNameExt();

            boolean canBeFound = CommonScripts.GRADLE_PROPERTIES_NAME.equalsIgnoreCase(baseName)
                || scriptProvider.isScriptFileName(baseName);
            // We have only gradle files and the gradle.properties.
            if (!canBeFound) {
                return null;
            }

            File userHome = getGradleUserHomeFile();
            if (userHome == null) {
                // Most likely we could not create the nodes, so
                // don't bother looking at subnodes.
                return null;
            }

            FileObject userHomeObj = FileUtil.toFileObject(userHome);
            if (userHomeObj == null) {
                // The directory does not exist, so there should be
                // no valid node.
                return null;
            }

            if (!FileUtil.isParentOf(userHomeObj, targetFile)) {
                return null;
            }

            Node result = NodeUtils.findFileChildNode(root.getChildren(), targetFile);
            if (result != null) {
                return result;
            }

            return NodeUtils.askChildrenForTarget(root.getChildren(), target);
        }
    }

    private static class GradleHomeNodeChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory>
    implements
            RefreshableChildren {

        private final ScriptFileProvider scriptProvider;
        private final ListenerRegistrations listenerRefs;
        private final ProxyListenerRegistry<Runnable> userHomeChangeListeners;
        private volatile boolean hasInitDDirDisplayed;
        private volatile boolean createdOnce;

        public GradleHomeNodeChildFactory(ScriptFileProvider scriptProvider) {
            this.scriptProvider = Objects.requireNonNull(scriptProvider, "scriptProvider");
            this.listenerRefs = new ListenerRegistrations();
            this.userHomeChangeListeners = new ProxyListenerRegistry<>(NbListenerManagers.neverNotifingRegistry());
            this.hasInitDDirDisplayed = false;
            this.createdOnce = false;
        }

        @Override
        public void refreshChildren() {
            if (createdOnce) {
                refresh(false);
            }
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
                userHomeChangeListeners.replaceRegistry((Runnable listener) -> {
                    return NbFileUtils.addDirectoryContentListener(userHome, true, listener);
                });
            }
            userHomeChangeListeners.onEvent(EventListeners.runnableDispatcher(), null);
        }

        @Override
        protected void addNotify() {
            listenerRefs.add(GradleFileUtils.GRADLE_USER_HOME.addChangeListener(this::updateUserHome));
            updateUserHome();
            listenerRefs.add(userHomeChangeListeners.registerListener(() -> refresh(false)));
        }

        @Override
        protected void removeNotify() {
            listenerRefs.unregisterAll();
        }

        private static FileObject tryGetFile(Path dir, String name) {
            return FileUtil.toFileObject(dir.resolve(name).toFile());
        }

        private List<FileObject> getScriptFiles(Path dir, String baseName) {
            Iterable<Path> paths = scriptProvider.findScriptFiles(dir, baseName);

            List<FileObject> result = new ArrayList<>();
            for (Path path: paths) {
                FileObject fileObj = FileUtil.toFileObject(path.toFile());
                if (fileObj != null) {
                    result.add(fileObj);
                }
            }

            return result;
        }

        private void addGradleProperties(Path userHome, List<SingleNodeFactory> toPopulate) {
            FileObject gradleProperties = tryGetFile(userHome, CommonScripts.GRADLE_PROPERTIES_NAME);
            if (gradleProperties != null) {
                SingleNodeFactory node = NodeUtils.tryGetFileNode(gradleProperties);
                if (node != null) {
                    toPopulate.add(node);
                }
            }
        }

        private Collection<FileObject> addInitGradle(Path userHome, List<SingleNodeFactory> toPopulate) {
            List<FileObject> initGradles = getScriptFiles(userHome, INIT_GRADLE_BASE_NAME);
            for (FileObject initGradle: initGradles) {
                SingleNodeFactory node = NodeUtils.tryGetFileNode(
                        initGradle,
                        initGradle.getNameExt(),
                        NbIcons.getGradleIcon());
                if (node != null) {
                    toPopulate.add(node);
                }
            }
            return initGradles;
        }

        private void addInitDDir(Path userHome, List<SingleNodeFactory> toPopulate) {
            final FileObject initD = tryGetFile(userHome, INIT_D_NAME);

            if (initD != null && initD.isFolder()) {
                hasInitDDirDisplayed = true;
                toPopulate.add(GradleFolderNode.getFactory(
                        NbStrings.getGlobalInitScriptsNodeCaption(),
                        initD,
                        scriptProvider));
            }
        }

        private void addOtherGradleFiles(
                Path userHome,
                Collection<FileObject> filtered,
                List<SingleNodeFactory> toPopulate) {
            FileObject userHomeObj = FileUtil.toFileObject(userHome.toFile());
            if (userHomeObj == null) {
                return;
            }

            List<FileObject> gradleFiles = new ArrayList<>();
            for (FileObject file: userHomeObj.getChildren()) {
                if (filtered.contains(file)) {
                    continue;
                }

                if (scriptProvider.isScriptFileName(file.getNameExt())) {
                    gradleFiles.add(file);
                }
            }

            Collections.sort(gradleFiles, Comparator.comparing(FileObject::getNameExt, StringUtils.STR_CMP::compare));

            for (FileObject file: gradleFiles) {
                SingleNodeFactory node = NodeUtils.tryGetFileNode(
                        file,
                        file.getNameExt(),
                        NbIcons.getGradleIcon());
                if (node != null) {
                    toPopulate.add(node);
                }
            }
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            hasInitDDirDisplayed = false;

            Path userHome = getGradleUserHome();
            if (userHome == null) {
                return;
            }

            addGradleProperties(userHome, toPopulate);
            Collection<FileObject> initGradles = addInitGradle(userHome, toPopulate);
            addInitDDir(userHome, toPopulate);

            addOtherGradleFiles(userHome, initGradles, toPopulate);
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

    private static final class FactoryImpl implements SingleNodeFactory {
        private final ScriptFileProvider scriptProvider;

        public FactoryImpl(ScriptFileProvider scriptProvider) {
            this.scriptProvider = Objects.requireNonNull(scriptProvider, "scriptProvider");
        }

        @Override
        public Node createNode() {
            return new GradleHomeNode(scriptProvider);
        }
    }

    @SuppressWarnings("serial")
    private static class CreateInitDAction extends AbstractAction {
        public CreateInitDAction() {
            super(NbStrings.getCreateInitDDir());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NbTaskExecutors.DEFAULT_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, (cancelToken) -> {
                Path userHome = getGradleUserHome();
                if (userHome != null) {
                    Path initDPath = userHome.resolve(INIT_D_NAME);
                    Files.createDirectories(initDPath);
                }
            }).exceptionally(AsyncTasks::expectNoError);
        }
    }
}
