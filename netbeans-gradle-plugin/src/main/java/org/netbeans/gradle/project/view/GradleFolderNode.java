package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.filesystems.FileChangeAdapter;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileEvent;
import org.openide.filesystems.FileObject;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public final class GradleFolderNode extends AbstractNode {
    private final String caption;

    public GradleFolderNode(String caption, FileObject dir) {
        super(createChildren(dir));
        ExceptionHelper.checkNotNullArgument(caption, "caption");

        this.caption = caption;
    }

    private static Children createChildren(FileObject dir) {
        return Children.create(new ChildFactoryImpl(dir), true);
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
        return caption;
    }

    private static class ChildFactoryImpl
    extends
            ChildFactory.Detachable<SingleNodeFactory> {
        private final FileObject dir;
        private final FileChangeListener changeListener;

        public ChildFactoryImpl(FileObject dir) {
            ExceptionHelper.checkNotNullArgument(dir, "dir");

            this.dir = dir;
            this.changeListener = new FileChangeAdapter() {
                @Override
                public void fileDeleted(FileEvent fe) {
                    refresh(false);
                }

                @Override
                public void fileDataCreated(FileEvent fe) {
                    refresh(false);
                }
            };
        }

        @Override
        protected void addNotify() {
            dir.addFileChangeListener(changeListener);
        }

        @Override
        protected void removeNotify() {
            dir.removeFileChangeListener(changeListener);
        }

        private static SingleNodeFactory tryGetGradleNode(FileObject child) {
            return NodeUtils.tryGetFileNode(child, child.getNameExt(), NbIcons.getGradleIcon());
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            FileObject[] children = dir.getChildren();
            Arrays.sort(children, new Comparator<FileObject>() {
                @Override
                public int compare(FileObject o1, FileObject o2) {
                    return StringUtils.STR_CMP.compare(o1.getNameExt(), o2.getNameExt());
                }
            });

            for (FileObject child: children) {
                String ext = child.getExt();
                if (SettingsFiles.DEFAULT_GRADLE_EXTENSION_WITHOUT_DOT.equalsIgnoreCase(ext)) {
                    SingleNodeFactory node = tryGetGradleNode(child);
                    if (node != null) {
                        toPopulate.add(node);
                    }
                }
            }
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
