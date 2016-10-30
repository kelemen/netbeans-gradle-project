package org.netbeans.gradle.project;

import java.awt.Image;
import java.beans.BeanInfo;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.ImageIcon;
import javax.swing.UIManager;
import org.netbeans.api.annotations.common.StaticResource;
import org.openide.awt.NotificationDisplayer;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataFolder;
import org.openide.nodes.Node;
import org.openide.util.ImageUtilities;

public final class NbIcons {
    private static final Logger LOGGER = Logger.getLogger(NbIcons.class.getName());

    @StaticResource
    public static final String PROJECT_ICON_PATH = "org/netbeans/gradle/project/resources/gradle.png";

    @StaticResource
    private static final String TASK_ICON_PATH = "org/netbeans/gradle/project/resources/task.gif";

    @StaticResource
    private static final String LIBRARIES_BADGE_ICON_PATH = "org/netbeans/gradle/project/resources/libraries-badge.png";

    @StaticResource
    private static final String PACKAGE_BADGE_ICON_PATH = "org/netbeans/gradle/project/resources/package-badge.png";

    @StaticResource
    private static final String LIBRARY_ICON_PATH = "org/netbeans/gradle/project/resources/libraries.png";

    @StaticResource
    private static final String WARNING_BADGE_ICON_PATH = "org/netbeans/gradle/project/resources/warning-badge.png";

    public static Image getGradleIcon() {
        return ImageUtilities.loadImage(PROJECT_ICON_PATH);
    }

    public static Icon getGradleIconAsIcon() {
        return ImageUtilities.loadImageIcon(PROJECT_ICON_PATH, true);
    }

    public static Image getTaskIcon() {
        return ImageUtilities.loadImage(TASK_ICON_PATH);
    }

    public static Image getLibraryIcon() {
        return ImageUtilities.loadImage(LIBRARY_ICON_PATH);
    }

    public static Image getPackageBadge() {
        return ImageUtilities.loadImage(PACKAGE_BADGE_ICON_PATH);
    }

    public static Image getLibrariesBadge() {
        return ImageUtilities.loadImage(LIBRARIES_BADGE_ICON_PATH);
    }

    public static Image getWarningBadge() {
        return ImageUtilities.loadImage(WARNING_BADGE_ICON_PATH);
    }

    public static Icon getPriorityHighIcon() {
        return NotificationDisplayer.Priority.HIGH.getIcon();
    }

    public static Icon getUIQuestionIcon() {
        return UIManager.getIcon("OptionPane.questionIcon");
    }

    public static Icon getUIWarningIcon() {
        return UIManager.getIcon("OptionPane.warningIcon");
    }

    public static Icon getUIErrorIcon() {
        return UIManager.getIcon("OptionPane.errorIcon");
    }

    private static class OpenFolderIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            Node n = DataFolder.findFolder(FileUtil.getConfigRoot()).getNodeDelegate();
            ImageIcon original = new ImageIcon(n.getOpenedIcon(BeanInfo.ICON_COLOR_16x16));
            Image result = original.getImage();
            if (result == null) {
                LOGGER.warning("Failed to load the open folder icon.");
            }
            return result;
        }
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

    public static Image getOpenFolderIcon() {
        return OpenFolderIconHolder.IMAGE;
    }

    public static Image getFolderIcon() {
        return FolderIconHolder.IMAGE;
    }

    private static class PackageIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            return mergeFolderWithBadge(false, getPackageBadge());
        }
    }

    private static class OpenPackageIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            return mergeFolderWithBadge(true, getPackageBadge());
        }
    }

    public static Image getOpenPackageIcon() {
        return OpenPackageIconHolder.IMAGE;
    }

    public static Image getPackageIcon() {
        return PackageIconHolder.IMAGE;
    }

    private static class LibrariesIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            return mergeFolderWithBadge(false, getLibrariesBadge());
        }
    }

    private static class OpenLibrariesIconHolder {
        public static final Image IMAGE = loadIcon();

        private static Image loadIcon() {
            return mergeFolderWithBadge(true, getLibrariesBadge());
        }
    }

    private static Image mergeFolderWithBadge(boolean opened, Image badge) {
        Image folderIcon = opened ? getOpenFolderIcon() : getFolderIcon();
        if (folderIcon != null && badge != null) {
            return ImageUtilities.mergeImages(folderIcon, badge, 7, 7);
        }
        else {
            return null;
        }
    }

    public static Image getOpenLibrariesIcon() {
        return OpenLibrariesIconHolder.IMAGE;
    }

    public static Image getLibrariesIcon() {
        return LibrariesIconHolder.IMAGE;
    }

    private NbIcons() {
        throw new AssertionError();
    }
}
