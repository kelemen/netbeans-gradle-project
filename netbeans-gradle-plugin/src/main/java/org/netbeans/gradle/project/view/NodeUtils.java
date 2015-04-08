package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.util.Objects;
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

    private NodeUtils() {
        throw new AssertionError();
    }
}
