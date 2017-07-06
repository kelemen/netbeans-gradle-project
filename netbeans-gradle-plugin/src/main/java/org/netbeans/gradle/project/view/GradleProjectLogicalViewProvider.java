package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Logger;
import javax.swing.Action;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.ProjectDisplayInfo;
import org.netbeans.gradle.project.ProjectIssue;
import org.netbeans.gradle.project.ProjectIssue.Kind;
import org.netbeans.gradle.project.api.nodes.NodeRefresher;
import org.netbeans.gradle.project.model.ModelRefreshListener;
import org.netbeans.gradle.project.util.ArrayUtils;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.ui.LogicalViewProvider;
import org.netbeans.spi.project.ui.PathFinder;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeAdapter;
import org.openide.nodes.NodeEvent;
import org.openide.util.ImageUtilities;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class GradleProjectLogicalViewProvider
implements
        LogicalViewProvider, ModelRefreshListener {
    private static final Logger LOGGER = Logger.getLogger(GradleProjectLogicalViewProvider.class.getName());

    private final NbGradleProject project;
    private final ContextActionProvider actionProvider;

    private final ListenerManager<ModelRefreshListener> childRefreshListeners;
    private final AtomicReference<Collection<ModelRefreshListener>> listenersToFinalize;

    public GradleProjectLogicalViewProvider(NbGradleProject project, ContextActionProvider actionProvider) {
        this.project = Objects.requireNonNull(project, "project");
        this.actionProvider = Objects.requireNonNull(actionProvider, "actionProvider");
        this.childRefreshListeners = new CopyOnTriggerListenerManager<>();
        this.listenersToFinalize = new AtomicReference<>(null);
    }

    public ListenerRef addChildModelRefreshListener(final ModelRefreshListener listener) {
        Objects.requireNonNull(listener, "listener");
        return childRefreshListeners.registerListener(listener);
    }

    @Override
    public void startRefresh() {
        final List<ModelRefreshListener> listeners = new ArrayList<>();
        childRefreshListeners.onEvent((ModelRefreshListener eventListener, Void arg) -> {
            eventListener.startRefresh();
            listeners.add(eventListener);
        }, null);

        Collection<ModelRefreshListener> prevListeners = listenersToFinalize.getAndSet(listeners);
        if (prevListeners != null) {
            LOGGER.warning("startRefresh/endRefresh mismatch.");
        }
    }

    @Override
    public void endRefresh(boolean extensionsChanged) {
        Collection<ModelRefreshListener> listeners = listenersToFinalize.getAndSet(null);
        if (listeners == null) {
            return;
        }

        for (ModelRefreshListener listener: listeners) {
            listener.endRefresh(extensionsChanged);
        }
    }

    @Override
    public Node createLogicalView() {
        DataFolder projectFolder = DataFolder.findFolder(project.getProjectDirectory());

        GradleProjectNode result = new GradleProjectNode(projectFolder, actionProvider);
        ProjectDisplayInfo displayInfo = project.getDisplayInfo();

        final ListenerRef displayNameRef = displayInfo.displayName().addChangeListener(result::fireDisplayNameChange);
        final ListenerRef descriptionRef = displayInfo.description().addChangeListener(result::fireShortDescriptionChange);
        final ListenerRef modelListenerRef = project.currentModel().addChangeListener(result::fireModelChange);
        final ListenerRef infoListenerRef = project.getProjectIssueManager().addChangeListener(result::fireInfoChangeEvent);
        result.addNodeListener(new NodeAdapter(){
            @Override
            public void nodeDestroyed(NodeEvent ev) {
                displayNameRef.unregister();
                descriptionRef.unregister();
                infoListenerRef.unregister();
                modelListenerRef.unregister();
            }
        });

        return result;
    }

    private Lookup createLookup(GradleProjectChildFactory childFactory, Children children, Object... extraServices) {
        NodeRefresher nodeRefresher = NodeUtils.defaultNodeRefresher(children, childFactory);
        Object[] services = ArrayUtils.concatArrays(
                new Object[]{nodeRefresher, project.getProjectDirectory()},
                extraServices);
        return new ProxyLookup(project.getLookup(), Lookups.fixed(services));
    }

    private final class GradleProjectNode extends FilterNode {
        @SuppressWarnings("VolatileArrayField")
        private volatile Action[] actions;

        private final ContextActionProvider actionProvider;

        private final UpdateTaskExecutor modelChanges;
        private final UpdateTaskExecutor displayNameChanges;
        private final UpdateTaskExecutor descriptionChanges;
        private final UpdateTaskExecutor iconChanges;

        public GradleProjectNode(DataFolder projectFolder, ContextActionProvider actionProvider) {
            this(projectFolder, actionProvider, new GradleProjectChildFactory(project, GradleProjectLogicalViewProvider.this));
        }

        private GradleProjectNode(DataFolder projectFolder, ContextActionProvider actionProvider, GradleProjectChildFactory childFactory) {
            this(projectFolder, actionProvider, childFactory, Children.create(childFactory, false));
        }

        private GradleProjectNode(
                DataFolder projectFolder,
                ContextActionProvider actionProvider,
                GradleProjectChildFactory childFactory,
                org.openide.nodes.Children children) {
            // Do not add lookup of "node" because that might fool NB to believe that multiple projects are selected.
            super(projectFolder.getNodeDelegate().cloneNode(), children, createLookup(childFactory, children, projectFolder));

            this.actionProvider = actionProvider;
            // TODO: It would be nicer to lazily initialize this list.
            this.actions = actionProvider.getActions();
            this.modelChanges = SwingExecutors.getSwingUpdateExecutor(true);
            this.displayNameChanges = SwingExecutors.getSwingUpdateExecutor(true);
            this.descriptionChanges = SwingExecutors.getSwingUpdateExecutor(true);
            this.iconChanges = SwingExecutors.getSwingUpdateExecutor(true);
        }

        private void updateActionsList() {
            actions = actionProvider.getActions();
        }

        public void fireDisplayNameChange() {
            displayNameChanges.execute(() -> fireDisplayNameChange(null, null));
        }

        public void fireShortDescriptionChange() {
            descriptionChanges.execute(() -> fireShortDescriptionChange(null, null));
        }

        public void fireModelChange() {
            modelChanges.execute(this::updateActionsList);
        }

        public void fireInfoChangeEvent() {
            iconChanges.execute(() -> {
                fireIconChange();
                fireOpenedIconChange();
            });
        }

        @Override
        public Action[] getActions(boolean context) {
            return actions.clone();
        }

        private void appendHtmlList(String caption, List<String> toAdd, StringBuilder result) {
            if (toAdd.isEmpty()) {
                return;
            }

            result.append("<B>");
            result.append(caption);
            result.append("</B>:");
            result.append("<ul>\n");
            for (String info: toAdd) {
                result.append("<li>");
                // TODO: quote strings, so that they are valid for html
                result.append(info);
                result.append("</li>\n");
            }
            result.append("</ul>\n");
        }

        @Override
        public Image getIcon(int type) {
            Image icon = NbIcons.getGradleIcon();
            Collection<ProjectIssue> infos = project.getProjectIssueManager().getIssues();
            if (!infos.isEmpty()) {
                Map<ProjectIssue.Kind, List<String>> infoMap
                        = new EnumMap<>(ProjectIssue.Kind.class);

                for (ProjectIssue.Kind kind: ProjectIssue.Kind.values()) {
                    infoMap.put(kind, new ArrayList<>());
                }

                Kind mostImportantKind = Kind.INFO;
                for (ProjectIssue info: infos) {
                    for (ProjectIssue.Entry entry: info.getEntries()) {
                        Kind kind = entry.getKind();
                        if (mostImportantKind.getImportance() < kind.getImportance()) {
                            mostImportantKind = kind;
                        }
                        infoMap.get(kind).add(entry.getSummary());
                    }
                }

                StringBuilder completeText = new StringBuilder(1024);
                appendHtmlList(NbStrings.getErrorCaption(), infoMap.get(ProjectIssue.Kind.ERROR), completeText);
                appendHtmlList(NbStrings.getWarningCaption(), infoMap.get(ProjectIssue.Kind.WARNING), completeText);
                appendHtmlList(NbStrings.getInfoCaption(), infoMap.get(ProjectIssue.Kind.INFO), completeText);

                icon = ImageUtilities.addToolTipToImage(icon, completeText.toString());
            }
            return icon;
        }

        @Override
        public Image getOpenedIcon(int type) {
            return getIcon(type);
        }

        @Override
        public String getDisplayName() {
            return project.getDisplayName();
        }

        @Override
        public String getShortDescription() {
            return project.getDisplayInfo().description().getValue();
        }
    }

    @Override
    public Node findPath(Node root, Object target) {
        if (target == null) {
            return null;
        }

        FileObject targetFile = NodeUtils.tryGetFileSearchTarget(target);

        Node[] children = root.getChildren().getNodes(true);
        for (Node child: children) {
            boolean hasNodeFinder = false;
            for (PathFinder nodeFinder: child.getLookup().lookupAll(PathFinder.class)) {
                hasNodeFinder = true;

                Node result = nodeFinder.findPath(child, target);
                if (result != null) {
                    return result;
                }
            }

            if (hasNodeFinder) {
                continue;
            }

            // This will always return {@code null} because PackageView
            // asks for PathFinder as well but since it is not in its
            // specification, we won't rely on this.
            Node result = PackageView.findPath(child, target);
            if (result == null && targetFile != null) {
                result = NodeUtils.findChildFileOfFolderNode(child, targetFile);
            }

            if (result != null) {
                return result;
            }
        }

        return null;
    }
}
