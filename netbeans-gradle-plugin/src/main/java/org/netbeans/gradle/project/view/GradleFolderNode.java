package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.StringUtils;
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

    public static SingleNodeFactory getFactory(String caption, FileObject dir) {
        ExceptionHelper.checkNotNullArgument(caption, "caption");
        ExceptionHelper.checkNotNullArgument(dir, "dir");

        return new FactoryImpl(caption, dir);
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
        private final ListenerRegistrations listenerRegistrations;

        public ChildFactoryImpl(FileObject dir) {
            ExceptionHelper.checkNotNullArgument(dir, "dir");

            this.dir = dir;
            this.listenerRegistrations = new ListenerRegistrations();
        }

        @Override
        protected void addNotify() {
            listenerRegistrations.add(NbFileUtils.addDirectoryContentListener(dir, new Runnable() {
                @Override
                public void run() {
                    refresh(false);
                }
            }));
        }

        @Override
        protected void removeNotify() {
            listenerRegistrations.unregisterAll();
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

    private static final class FactoryImpl implements SingleNodeFactory {
        private final String caption;
        private final FileObject dir;

        public FactoryImpl(String caption, FileObject dir) {
            this.caption = caption;
            this.dir = dir;
        }

        @Override
        public Node createNode() {
            return new GradleFolderNode(caption, dir);
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 83 * hash + Objects.hashCode(this.caption);
            hash = 83 * hash + Objects.hashCode(this.dir);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final FactoryImpl other = (FactoryImpl)obj;
            return Objects.equals(this.caption, other.caption)
                    && Objects.equals(this.dir, other.dir);
        }
    }
}
