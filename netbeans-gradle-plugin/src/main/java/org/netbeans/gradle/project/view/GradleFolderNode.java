package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.awt.event.ActionEvent;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;
import org.jtrim2.executor.TaskExecutor;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.filesupport.GradleTemplateConsts;
import org.netbeans.gradle.project.filesupport.GradleTemplateRegistration;
import org.netbeans.gradle.project.output.OpenEditorOutputListener;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.util.RefreshableChildren;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class GradleFolderNode extends AbstractNode {
    private static final TaskExecutor GRADLE_FOLDER_CREATOR
            = NbTaskExecutors.newExecutor("Gradle folder creator", 1);

    private final String caption;
    private final FileObject dir;

    public GradleFolderNode(String caption, FileObject dir, ScriptFileProvider scriptProvider) {
        this(caption, dir, new ChildFactoryImpl(dir, scriptProvider));
    }

    private GradleFolderNode(
            String caption,
            FileObject dir,
            ChildFactoryImpl childFactory) {
        this(caption, dir, childFactory, Children.create(childFactory, true));
    }

    private GradleFolderNode(
            String caption,
            FileObject dir,
            ChildFactoryImpl childFactory,
            Children children) {
        super(children, createLookup(childFactory, children));

        this.caption = Objects.requireNonNull(caption, "caption");
        this.dir = dir;

        setName(dir.toString());
    }

    public static SingleNodeFactory getFactory(String caption, FileObject dir, ScriptFileProvider scriptProvider) {
        return new FactoryImpl(caption, dir, scriptProvider);
    }

    private static Lookup createLookup(ChildFactoryImpl childFactory, Children children) {
        return Lookups.fixed(
                NodeUtils.childrenFileFinder(),
                NodeUtils.defaultNodeRefresher(children, childFactory));
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
        return caption;
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            new CreateGradleFileTask(dir),
            null,
            NodeUtils.getRefreshNodeAction(this)
        };
    }

    @SuppressWarnings("serial")
    private static class CreateGradleFileTask extends AbstractAction {
        private final FileObject dir;

        public CreateGradleFileTask(FileObject dir) {
            super(NbStrings.getAddNewInitScriptCaption());

            this.dir = dir;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final TemplateWizard template = new TemplateWizard();
            DataFolder targetFolder = DataFolder.findFolder(dir);
            template.setTargetFolder(targetFolder);
            template.setTemplatesFolder(DataFolder.findFolder(GradleTemplateConsts.getTemplateFolder()));

            GRADLE_FOLDER_CREATOR.execute(Cancellation.UNCANCELABLE_TOKEN, (CancellationToken cancelToken) -> {
                Set<DataObject> dataObjs = template.instantiate(DataFolder.find(GradleTemplateRegistration.getTemplateFileObj()));
                if (dataObjs == null) {
                    return;
                }

                for (DataObject dataObj: dataObjs) {
                    final FileObject fileObj = dataObj.getPrimaryFile();
                    if (fileObj != null) {
                        SwingUtilities.invokeLater(() -> {
                            OpenEditorOutputListener.tryOpenFile(fileObj, -1);
                        });
                    }
                }
            }).exceptionally(AsyncTasks::expectNoError);
        }
    }

    private static class ChildFactoryImpl
    extends
            ChildFactory.Detachable<SingleNodeFactory>
    implements
            RefreshableChildren {
        private final FileObject dir;
        private final ScriptFileProvider scriptProvider;
        private final ListenerRegistrations listenerRegistrations;
        private volatile boolean createdOnce;

        public ChildFactoryImpl(FileObject dir, ScriptFileProvider scriptProvider) {
            this.dir = Objects.requireNonNull(dir, "dir");
            this.scriptProvider = Objects.requireNonNull(scriptProvider, "scriptProvider");
            this.listenerRegistrations = new ListenerRegistrations();
            this.createdOnce = false;
        }

        @Override
        public void refreshChildren() {
            if (createdOnce) {
                refresh(false);
            }
        }

        @Override
        protected void addNotify() {
            listenerRegistrations.add(NbFileUtils.addDirectoryContentListener(dir, this::refreshChildren));
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
            Arrays.sort(children, Comparator.comparing(FileObject::getNameExt, StringUtils.STR_CMP::compare));

            for (FileObject child: children) {
                if (scriptProvider.isScriptFileName(child.getNameExt())) {
                    SingleNodeFactory node = tryGetGradleNode(child);
                    if (node != null) {
                        toPopulate.add(node);
                    }
                }
            }
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
        private final String caption;
        private final FileObject dir;
        private final ScriptFileProvider scriptProvider;

        public FactoryImpl(String caption, FileObject dir, ScriptFileProvider scriptProvider) {
            this.caption = Objects.requireNonNull(caption, "caption");
            this.dir = Objects.requireNonNull(dir, "dir");
            this.scriptProvider = Objects.requireNonNull(scriptProvider, "scriptProvider");
        }

        @Override
        public Node createNode() {
            return new GradleFolderNode(caption, dir, scriptProvider);
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
