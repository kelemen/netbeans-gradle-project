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
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.filesupport.GradleTemplateConsts;
import org.netbeans.gradle.project.filesupport.GradleTemplateRegistration;
import org.netbeans.gradle.project.output.OpenEditorOutputListener;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.WizardDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataFolder;
import org.openide.loaders.DataObject;
import org.openide.loaders.TemplateWizard;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;
import org.openide.util.lookup.Lookups;

public final class GradleFolderNode extends AbstractNode {
    private static final TaskExecutor GRADLE_FOLDER_CREATOR
            = NbTaskExecutors.newExecutor("Gradle folder creator", 1);

    private final String caption;
    private final FileObject dir;

    public GradleFolderNode(String caption, FileObject dir) {
        super(createChildren(dir), Lookups.fixed(NodeUtils.childrenFileFinder()));
        ExceptionHelper.checkNotNullArgument(caption, "caption");

        this.caption = caption;
        this.dir = dir;

        setName(dir.toString());
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

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            new CreateGradleFileTask(dir)
        };
    }

    @SuppressWarnings("serial")
    private static class CreateGradleFileTask extends AbstractAction {
        private final FileObject dir;

        public CreateGradleFileTask(FileObject dir) {
            // TODO: I18N
            super("Add new init script");

            this.dir = dir;
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            final TemplateWizard template = new TemplateWizard();
            DataFolder targetFolder = DataFolder.findFolder(dir);
            template.setTargetFolder(targetFolder);
            template.setTemplatesFolder(DataFolder.findFolder(GradleTemplateConsts.getTemplateFolder()));

            GRADLE_FOLDER_CREATOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) throws Exception {
                    Set<DataObject> dataObjs = template.instantiate(DataFolder.find(GradleTemplateRegistration.getTemplateFileObj()));
                    if (dataObjs == null) {
                        return;
                    }

                    for (DataObject dataObj: dataObjs) {
                        final FileObject fileObj = dataObj.getPrimaryFile();
                        if (fileObj != null) {
                            SwingUtilities.invokeLater(new Runnable() {
                                @Override
                                public void run() {
                                    OpenEditorOutputListener.tryOpenFile(fileObj, -1);
                                }
                            });
                        }
                    }
                }
            }, null);
        }
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

    private static class NewGradleFileWizard extends TemplateWizard {

        @Override
        protected Panel<WizardDescriptor> createTemplateChooser() {
            return super.createTemplateChooser();
        }

        @Override
        protected Panel<WizardDescriptor> createTargetChooser() {
            return super.createTargetChooser();
        }

    }
}
