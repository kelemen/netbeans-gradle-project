package org.netbeans.gradle.project;

import java.awt.Image;
import java.beans.BeanInfo;
import java.util.logging.Logger;
import javax.swing.ImageIcon;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;

public final class NbIcons {
    private static final Logger LOGGER = Logger.getLogger(NbIcons.class.getName());

    @StaticResource
    private static final String PROJECT_ICON_PATH = "org/netbeans/gradle/project/resources/gradle.png";

    @StaticResource
    private static final String TASK_ICON_PATH = "org/netbeans/gradle/project/resources/task.gif";

    @StaticResource
    private static final String LIBRARIES_BADGE_ICON_PATH = "org/netbeans/gradle/project/resources/libraries-badge.png";

    @StaticResource
    private static final String LIBRARY_ICON_PATH = "org/netbeans/gradle/project/resources/libraries.png";

    private static class GradleIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            Image result = ImageUtilities.loadImage(PROJECT_ICON_PATH);
            if (result == null) {
                LOGGER.warning("Failed to load the Gradle icon.");
            }
            return result;
        }
    }

    public static Image getGradleIcon() {
        return GradleIconHolder.IMAGE;
    }

    private static class TaskIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            Image result = ImageUtilities.loadImage(TASK_ICON_PATH);
            if (result == null) {
                LOGGER.warning("Failed to load the task icon.");
            }
            return result;
        }
    }

    public static Image getTaskIcon() {
        return TaskIconHolder.IMAGE;
    }

    private static class LibraryIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            Image result = ImageUtilities.loadImage(LIBRARY_ICON_PATH);
            if (result == null) {
                LOGGER.warning("Failed to load the library icon.");
            }
            return result;
        }
    }

    public static Image getLibraryIcon() {
        return LibraryIconHolder.IMAGE;
    }

    private static class LibrariesBadgeHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            Image result = ImageUtilities.loadImage(LIBRARIES_BADGE_ICON_PATH);
            if (result == null) {
                LOGGER.warning("Failed to load the libraries badge.");
            }
            return result;
        }
    }

    public static Image getLibrariesBadge() {
        return LibrariesBadgeHolder.IMAGE;
    }

    private static class FolderIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            Node n = DataFolder.findFolder(FileUtil.getConfigRoot()).getNodeDelegate();
            ImageIcon original = new ImageIcon(n.getIcon(BeanInfo.ICON_COLOR_16x16));
            Image result = original.getImage();
            if (result == null) {
                LOGGER.warning("Failed to load the folder icon.");
            }
            return result;
        }
    }

    public static Image getFolderIcon() {
        return FolderIconHolder.IMAGE;
    }

    private static class LibrariesIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            Image folderIcon = getFolderIcon();
            Image badge = getLibrariesBadge();
            if (folderIcon != null && badge != null) {
                return ImageUtilities.mergeImages(folderIcon, badge, 7, 7);
            }
            else {
                LOGGER.warning("Failed to load the libraries icon.");
                return null;
            }
        }
    }

    public static Image getLibrariesIcon() {
        return LibrariesIconHolder.IMAGE;
    }

    private NbIcons() {
        throw new AssertionError();
    }
}
