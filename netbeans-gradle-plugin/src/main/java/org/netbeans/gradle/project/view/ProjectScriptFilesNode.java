package org.netbeans.gradle.project.view;

import java.awt.Image;
import java.io.File;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.Action;
import org.jtrim.event.ProxyListenerRegistry;
import org.jtrim.event.SimpleListenerRegistry;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.nodes.SingleNodeFactory;
import org.netbeans.gradle.project.event.NbListenerManagers;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.properties.SettingsFiles;
import org.netbeans.gradle.project.query.GradleFilesClassPathProvider;
import org.netbeans.gradle.project.util.ListenerRegistrations;
import org.netbeans.gradle.project.util.StringUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.nodes.AbstractNode;
import org.openide.nodes.ChildFactory;
import org.openide.nodes.Children;
import org.openide.nodes.Node;

public final class ProjectScriptFilesNode extends AbstractNode {
    private final String caption;
    private final File projectDir;

    public ProjectScriptFilesNode(String caption, File projectDir) {
        super(createChildren(projectDir));

        ExceptionHelper.checkNotNullArgument(caption, "caption");
        this.caption = caption;
        this.projectDir = projectDir;
    }

    public static SingleNodeFactory getFactory(String caption, File projectDir) {
        return new FactoryImpl(caption, projectDir);
    }

    private static Children createChildren(File projectDir) {
        return Children.create(new ProjectScriptFilesChildFactory(projectDir), true);
    }

    private Action openProjectFileAction(String name) {
        File file = new File(projectDir, name);

        String actionCaption = NbStrings.getOpenFileCaption(name);
        Action result = new OpenAlwaysFileAction(actionCaption, file.toPath());

        return result;
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            openProjectFileAction(SettingsFiles.GRADLE_PROPERTIES_NAME)
        };
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

    private static class ProjectScriptFilesChildFactory
    extends
            ChildFactory.Detachable<SingleNodeFactory> {
        private final File projectDir;
        private final AtomicReference<NbGradleProject> projectRef;
        private final ListenerRegistrations listenerRefs;
        private final ProxyListenerRegistry<Runnable> modelChangeListener;

        public ProjectScriptFilesChildFactory(File projectDir) {
            ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");
            this.projectDir = projectDir;
            this.projectRef = new AtomicReference<>(null);
            this.listenerRefs = new ListenerRegistrations();
            this.modelChangeListener = new ProxyListenerRegistry<>(NbListenerManagers.neverNotifingRegistry());
        }

        private NbGradleProject tryGetProject() {
            NbGradleProject result = projectRef.get();
            if (result == null) {
                Project rawProject = NbGradleProjectFactory.tryLoadSafeProject(projectDir);
                result = rawProject.getLookup().lookup(NbGradleProject.class);
                if (projectRef.compareAndSet(null, result)) {
                    SimpleListenerRegistry<Runnable> projectModelChangeListener
                            = NbProperties.asChangeListenerRegistry(result.currentModel());
                    modelChangeListener.replaceRegistry(projectModelChangeListener);
                    // There is no reason to notify listeners yet on the
                    // first project request because there is no change
                    // to observe yet.
                }
                else {
                    result = projectRef.get();
                }
            }
            return result;
        }

        @Override
        protected void addNotify() {
            listenerRefs.add(modelChangeListener.registerListener(new Runnable() {
                @Override
                public void run() {
                    // TODO: Refresh only if something relevant changed.
                    refresh(false);
                }
            }));
        }

        @Override
        protected void removeNotify() {
            listenerRefs.unregisterAll();
        }

        private void addFileObject(
                FileObject file,
                List<SingleNodeFactory> toPopulate) {
            SingleNodeFactory nodeFactory = NodeUtils.tryGetFileNode(file);
            if (nodeFactory != null) {
                toPopulate.add(nodeFactory);
            }
        }

        private void addGradleFile(
                FileObject file,
                List<SingleNodeFactory> toPopulate) {
            addGradleFile(file, file.getNameExt(), toPopulate);
        }

        private void addGradleFile(
                FileObject file,
                final String name,
                List<SingleNodeFactory> toPopulate) {

            SingleNodeFactory nodeFactory = NodeUtils.tryGetFileNode(file, name, NbIcons.getGradleIcon());
            if (nodeFactory != null) {
                toPopulate.add(nodeFactory);
            }
        }

        private static File getLocalGradleProperties(NbGradleModel model) {
            return new File(model.getProjectDir(), SettingsFiles.GRADLE_PROPERTIES_NAME);
        }

        private static FileObject tryGetLocalGradlePropertiesObj(NbGradleModel model) {
            File result = getLocalGradleProperties(model);
            return FileUtil.toFileObject(result);
        }

        private void readKeys(List<SingleNodeFactory> toPopulate) {
            NbGradleProject project = tryGetProject();
            if (project == null) {
                return;
            }

            NbGradleModel model = project.currentModel().getValue();

            FileObject settingsGradle = model.tryGetSettingsFileObj();
            if (settingsGradle != null && model.isRootProject()) {
                addGradleFile(settingsGradle, toPopulate);
            }

            FileObject buildGradle = model.tryGetBuildFileObj();
            if (buildGradle != null) {
                addGradleFile(buildGradle, toPopulate);
            }

            FileObject propertiesFile = tryGetLocalGradlePropertiesObj(model);
            if (propertiesFile != null) {
                addFileObject(propertiesFile, toPopulate);
            }

            List<FileObject> gradleFiles = new LinkedList<>();
            for (FileObject file: project.getProjectDirectory().getChildren()) {
                if (file.equals(buildGradle) || file.equals(settingsGradle)) {
                    continue;
                }

                if (GradleFilesClassPathProvider.isGradleFile(file)) {
                    gradleFiles.add(file);
                }
            }

            Collections.sort(gradleFiles, new Comparator<FileObject>() {
                @Override
                public int compare(FileObject o1, FileObject o2) {
                    return StringUtils.STR_CMP.compare(o1.getNameExt(), o2.getNameExt());
                }
            });

            for (FileObject file: gradleFiles) {
                addGradleFile(file, toPopulate);
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

    private static class FactoryImpl implements SingleNodeFactory {
        private final String caption;
        private final File projectDir;

        public FactoryImpl(String caption, File projectDir) {
            ExceptionHelper.checkNotNullArgument(caption, "caption");
            ExceptionHelper.checkNotNullArgument(projectDir, "projectDir");

            this.caption = caption;
            this.projectDir = projectDir;
        }

        @Override
        public Node createNode() {
            return new ProjectScriptFilesNode(caption, projectDir);
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 61 * hash + Objects.hashCode(this.caption);
            hash = 61 * hash + Objects.hashCode(this.projectDir);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final FactoryImpl other = (FactoryImpl)obj;
            return Objects.equals(this.caption, other.caption)
                    && Objects.equals(this.projectDir, other.projectDir);
        }
    }
}
