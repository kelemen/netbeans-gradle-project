package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.nodes.FilterNode;
import org.openide.nodes.Node;

public final class NodeUtils {
    private static final Logger LOGGER = Logger.getLogger(NodeUtils.class.getName());

    public static DataObject tryGetDataObject(FileObject fileObj) {
        try {
            return DataObject.find(fileObj);
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

        return new SingleNodeFactory() {
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
        };
    }

    private NodeUtils() {
        throw new AssertionError();
    }
}
