package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.StringTokenizer;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.NodeRefresher;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.util.ContextUtils;
import org.netbeans.gradle.project.util.RefreshableChildren;
import org.netbeans.spi.java.project.support.ui.PackageView;
import org.netbeans.spi.project.ui.PathFinder;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.Children;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;
import org.openide.nodes.NodeNotFoundException;
import org.openide.nodes.NodeOp;

public final class NodeUtils {
    private static final Logger LOGGER = Logger.getLogger(NodeUtils.class.getName());

    public static NodeRefresher defaultNodeRefresher(
            final Children children,
            final RefreshableChildren childrenRefresher) {
        ExceptionHelper.checkNotNullArgument(children, "children");
        ExceptionHelper.checkNotNullArgument(childrenRefresher, "childrenRefresher");

        return new NodeRefresher() {
            @Override
            public void refreshNode() {
                childrenRefresher.refreshChildren();
                NodeUtils.refreshChildNodes(children);
            }
        };
    }

    public static Action getRefreshNodeAction(Node node) {
        return getRefreshNodeAction(node, NbStrings.getScanForChangesCaption());
    }

    public static Action getRefreshNodeAction(final Node node, String caption) {
        ExceptionHelper.checkNotNullArgument(node, "node");
        ExceptionHelper.checkNotNullArgument(caption, "caption");

        return new RefreshNodeAction(caption, node);
    }

    public static void refreshChildNodes(Children children) {
        ExceptionHelper.checkNotNullArgument(children, "children");

        for (Node child: children.getNodes(false)) {
            NodeRefresher refresher = child.getLookup().lookup(NodeRefresher.class);
            if (refresher != null) {
                refresher.refreshNode();
            }
        }
    }

    public static FileObject tryGetFileSearchTarget(Object target) {
        if (target instanceof FileObject) {
            return (FileObject)target;
        }

        if (target instanceof DataObject) {
            return ((DataObject)target).getPrimaryFile();
        }

        return null;
    }

    public static Node findFileChildNode(Children children, FileObject file) {
        ExceptionHelper.checkNotNullArgument(children, "children");
        ExceptionHelper.checkNotNullArgument(file, "file");

        for (Node child: children.getNodes(true)) {
            if (NodeUtils.isNodeOfFile(child, file)) {
                return child;
            }
        }

        return null;
    }

    public static boolean isNodeOfFile(Node node, FileObject file) {
        ExceptionHelper.checkNotNullArgument(node, "node");
        ExceptionHelper.checkNotNullArgument(file, "file");

        for (FileObject nodeFile: node.getLookup().lookupAll(FileObject.class)) {
            if (file.equals(nodeFile)) {
                return true;
            }
        }
        return false;
    }

    public static Node askChildrenPackageViewsForTarget(Children children, Object target) {
        ExceptionHelper.checkNotNullArgument(children, "children");
        ExceptionHelper.checkNotNullArgument(target, "target");

        for (Node child: children.getNodes(true)) {
            Node result = PackageView.findPath(child, target);
            if (result != null) {
                return result;
            }
        }

        return null;
    }

    public static Node askChildrenForTarget(Children children, Object target) {
        ExceptionHelper.checkNotNullArgument(children, "children");
        ExceptionHelper.checkNotNullArgument(target, "target");

        for (Node child: children.getNodes(true)) {
            for (PathFinder nodeFinder: child.getLookup().lookupAll(PathFinder.class)) {
                Node result = nodeFinder.findPath(child, target);
                if (result != null) {
                    return result;
                }
            }
        }

        return null;
    }

    public static PathFinder childrenFileFinder() {
        return ChildrenFileFinder.INSTANCE;
    }

    public static PathFinder askChildrenNodeFinder() {
        return AskChildrenNodeFinder.INSTANCE;
    }

    public static PathFinder askChildrenPackageViewsFinder() {
        return AskChildrenPackageViewsFinder.INSTANCE;
    }

    public static Node findChildFileOfFolderNode(Node folderNode, FileObject file) {
        ExceptionHelper.checkNotNullArgument(folderNode, "folderNode");
        ExceptionHelper.checkNotNullArgument(file, "file");

        // Copied from the LogicalViewProvider implementation of the Maven plugin

        FileObject xfo = ContextUtils.tryGetFileObjFromContext(folderNode.getLookup());
        if (xfo != null) {
            if ((xfo.equals(file))) {
                return folderNode;
            }
            else if (FileUtil.isParentOf(xfo, file)) {
                FileObject folder = file.isFolder() ? file : file.getParent();
                String relPath = FileUtil.getRelativePath(xfo, folder);
                List<String> path = new ArrayList<>();
                StringTokenizer strtok = new StringTokenizer(relPath, "/");
                while (strtok.hasMoreTokens()) {
                    String token = strtok.nextToken();
                    path.add(token);
                }
                try {
                    Node parentNode = folder.equals(xfo)
                            ? folderNode
                            : NodeOp.findPath(folderNode, Collections.enumeration(path));
                    if (file.isFolder()) {
                        return parentNode;
                    }
                    else {
                        Node[] childs = parentNode.getChildren().getNodes(true);
                        for (Node child: childs) {
                            FileObject childFile = ContextUtils.tryGetFileObjFromContextPreferData(child.getLookup());
                            if (childFile != null && childFile.getNameExt().equals(file.getNameExt())) {
                                return child;
                            }
                        }
                    }
                } catch (NodeNotFoundException e) {
                    // OK, never mind
                }
            }
        }
        return null;
    }

    public static DataObject tryGetDataObject(FileObject fileObj) {
        try {
            if (fileObj.isFolder()) {
                return DataFolder.findFolder(fileObj);
            }
            else {
                return DataObject.find(fileObj);
            }
        } catch (DataObjectNotFoundException ex) {
            LOGGER.log(Level.INFO, "Failed to find DataObject for file object: " + fileObj.getPath(), ex);
            return null;
        }
    }

    public static SingleNodeFactory tryGetFileNode(FileObject file) {
        return tryGetFileNode(file, file.getNameExt());
    }

    public static SingleNodeFactory tryGetFileNode(FileObject file, final String name) {
        return tryGetFileNode(file, name, null);
    }

    public static SingleNodeFactory tryGetFileNode(FileObject file, final String name, final Image icon) {
        final DataObject fileData = tryGetDataObject(file);
        if (fileData == null) {
            return null;
        }

        return new FileObjNodeFactory(file.toURI(), fileData, name, icon);
    }

    private static final class FileObjNodeFactory implements SingleNodeFactory {
        private final Object fileDataKey;
        private final DataObject fileData;
        private final String name;
        private final Image icon;

        public FileObjNodeFactory(
                Object fileDataKey,
                DataObject fileData,
                String name,
                Image icon) {
            this.fileDataKey = fileDataKey;
            this.fileData = fileData;
            this.name = name;
            this.icon = icon;
        }

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

                @Override
                public Image getIcon(int type) {
                    return icon != null ? icon : super.getIcon(type);
                }

                @Override
                public Image getOpenedIcon(int type) {
                    return icon != null ? icon : super.getOpenedIcon(type);
                }
            };
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.fileDataKey);
            hash = 83 * hash + Objects.hashCode(this.name);
            hash = 83 * hash + Objects.hashCode(this.icon);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final FileObjNodeFactory other = (FileObjNodeFactory)obj;
            return this.icon == other.icon
                    && Objects.equals(this.name, other.name)
                    && Objects.equals(this.fileDataKey, other.fileDataKey);
        }
    }

    private enum AskChildrenPackageViewsFinder implements PathFinder {
        INSTANCE;

        @Override
        public Node findPath(Node root, Object target) {
            return askChildrenPackageViewsForTarget(root.getChildren(), target);
        }
    }

    private enum AskChildrenNodeFinder implements PathFinder {
        INSTANCE;

        @Override
        public Node findPath(Node root, Object target) {
            return askChildrenForTarget(root.getChildren(), target);
        }
    }

    private NodeUtils() {
        throw new AssertionError();
    }

    private enum ChildrenFileFinder implements PathFinder {
        INSTANCE;

        @Override
        public Node findPath(Node root, Object target) {
            FileObject targetFile = tryGetFileSearchTarget(target);
            return targetFile != null ? findFileChildNode(root.getChildren(), targetFile) : null;
        }
    }

    @SuppressWarnings("serial")
    private static class RefreshNodeAction extends AbstractAction {
        private final Node node;

        public RefreshNodeAction(String name, Node node) {
            super(name);
            this.node = node;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NodeRefresher nodeRefresher = node.getLookup().lookup(NodeRefresher.class);
            if (nodeRefresher != null) {
                nodeRefresher.refreshNode();
            }
        }
    }
}
