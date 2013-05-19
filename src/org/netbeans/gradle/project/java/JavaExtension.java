package org.netbeans.gradle.project.java;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.java.classpath.GlobalPathRegistry;
import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.DynamicLookup;
import org.netbeans.gradle.project.GradleProjectSources;
import org.netbeans.gradle.project.ProjectInitListener;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModelUtils;
import org.netbeans.gradle.project.java.query.GradleAnnotationProcessingQuery;
import org.netbeans.gradle.project.java.query.GradleBinaryForSourceQuery;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.java.query.GradleProjectTemplates;
import org.netbeans.gradle.project.java.query.GradleSourceForBinaryQuery;
import org.netbeans.gradle.project.java.query.GradleSourceLevelQueryImplementation;
import org.netbeans.gradle.project.java.query.GradleUnitTestFinder;
import org.netbeans.gradle.project.java.query.J2SEPlatformFromScriptQueryImpl;
import org.netbeans.gradle.project.java.query.JavaExtensionNodes;
import org.netbeans.spi.project.ui.ProjectOpenedHook;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class JavaExtension implements GradleProjectExtension {
    private static final Logger LOGGER = Logger.getLogger(JavaExtension.class.getName());

    private static final Iterable<List<Class<?>>> NEEDED_MODELS
            = Collections.singleton(Collections.<Class<?>>singletonList(IdeaProject.class));

    private final Project project;
    private volatile NbJavaModel currentModel;

    private final GradleClassPathProvider cpProvider;

    private final AtomicReference<Lookup> lookupRef;
    private final DynamicLookup extensionLookup;
    private final Lookup protectedExtensionLookup;

    public JavaExtension(Project project) throws IOException {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.currentModel = NbJavaModelUtils.createEmptyModel(project.getProjectDirectory());
        this.cpProvider = new GradleClassPathProvider(this);
        this.lookupRef = new AtomicReference<Lookup>(null);
        this.extensionLookup = new DynamicLookup();
        this.protectedExtensionLookup = DynamicLookup.viewLookup(extensionLookup);
    }

    public boolean isOwnerProject(File file) {
        FileObject fileObj = FileUtil.toFileObject(file);
        return fileObj != null ? isOwnerProject(fileObj) : false;
    }

    public boolean isOwnerProject(FileObject file) {
        Project owner = FileOwnerQuery.getOwner(file);
        if (owner == null) {
            return false;
        }

        return project.getProjectDirectory().equals(owner.getProjectDirectory());
    }

    public NbJavaModel getCurrentModel() {
        return currentModel;
    }

    @Override
    public Iterable<List<Class<?>>> getGradleModels() {
        return NEEDED_MODELS;
    }

    private Lookup getStaticLookup() {
        Lookup lookup = lookupRef.get();
        if (lookup == null) {
            lookup = Lookups.fixed(
                    new GradleProjectSources(this),
                    cpProvider,
                    new GradleSourceLevelQueryImplementation(this),
                    new GradleUnitTestFinder(this),
                    new OpenHook(),
                    new GradleAnnotationProcessingQuery(),
                    new GradleSourceForBinaryQuery(this),
                    new GradleBinaryForSourceQuery(this),
                    new GradleProjectTemplates(),
                    new JavaExtensionNodes(this),
                    new J2SEPlatformFromScriptQueryImpl(this) // internal use only
                    );

            if (lookupRef.compareAndSet(null, lookup)) {
                for (ProjectInitListener listener: lookup.lookupAll(ProjectInitListener.class)) {
                    listener.onInitProject();
                }
            }
            lookup = lookupRef.get();
        }
        return lookup;
    }

    @Override
    public Lookup getExtensionLookup() {
        return protectedExtensionLookup;
    }

    public Project getProject() {
        return project;
    }

    public Lookup getProjectLookup() {
        return project.getLookup();
    }

    public FileObject getProjectDirectory() {
        return project.getProjectDirectory();
    }

    public String getName() {
        return currentModel.getMainModule().getShortName();
    }

    private void switchToEmptyModel() {
        currentModel = NbJavaModelUtils.createEmptyModel(currentModel.getMainModule().getModuleDir());
    }

    @Override
    public void modelsLoaded(Lookup modelLookup) {
        boolean loaded = false;

        NbJavaModel javaModel = modelLookup.lookup(NbJavaModel.class);
        if (javaModel == null) {
            IdeaProject ideaProject = modelLookup.lookup(IdeaProject.class);
            if (ideaProject == null) {
                switchToEmptyModel();
            }
            else {
                try {
                    currentModel = NbJavaModelUtils.parseFromIdeaModel(
                            currentModel.getMainModule().getModuleDir(),
                            ideaProject);
                    loaded = true;
                } catch (IOException ex) {
                    LOGGER.log(Level.WARNING, "Failed to parse model.", ex);
                    switchToEmptyModel();
                }
            }
        }
        else {
            currentModel = javaModel;
            loaded = true;
        }

        if (loaded) {
            extensionLookup.replaceLookups(getStaticLookup());
        }
        else {
            extensionLookup.replaceLookups();
        }

        for (JavaModelChangeListener listener: getExtensionLookup().lookupAll(JavaModelChangeListener.class)) {
            listener.onModelChange();
        }
    }

    // OpenHook is important for debugging because the debugger relies on the
    // globally registered source class paths for source stepping.

    // SwingUtilities.invokeLater is used only to guarantee the order of events.
    // Actually any executor which executes tasks in the order they were
    // submitted to it is good (using SwingUtilities.invokeLater was only
    // convenient to use because registering paths is cheap enough).
    private class OpenHook extends ProjectOpenedHook implements PropertyChangeListener {
        private final List<GlobalPathReg> paths;
        private boolean opened;

        public OpenHook() {
            this.opened = false;

            this.paths = new LinkedList<GlobalPathReg>();
            this.paths.add(new GlobalPathReg(ClassPath.SOURCE));
            this.paths.add(new GlobalPathReg(ClassPath.BOOT));
            this.paths.add(new GlobalPathReg(ClassPath.COMPILE));
            this.paths.add(new GlobalPathReg(ClassPath.EXECUTE));
        }

        @Override
        protected void projectOpened() {
            cpProvider.addPropertyChangeListener(this);

            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    opened = true;
                    doRegisterClassPaths();
                }
            });
        }

        @Override
        protected void projectClosed() {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    opened = false;
                    doUnregisterPaths();
                }
            });

            cpProvider.removePropertyChangeListener(this);
        }

        private void doUnregisterPaths() {
            assert SwingUtilities.isEventDispatchThread();

            for (GlobalPathReg pathReg: paths) {
                pathReg.unregister();
            }
        }

        private void doRegisterClassPaths() {
            assert SwingUtilities.isEventDispatchThread();

            for (GlobalPathReg pathReg: paths) {
                pathReg.register();
            }
        }

        @Override
        public void propertyChange(PropertyChangeEvent evt) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    if (opened) {
                        doRegisterClassPaths();
                    }
                }
            });
        }
    }

    private class GlobalPathReg {
        private final String type;
        // Note that using AtomicReference does not really make the methods
        // thread-safe but is only convenient to use.
        private final AtomicReference<ClassPath[]> paths;

        public GlobalPathReg(String type) {
            this.type = type;
            this.paths = new AtomicReference<ClassPath[]>(null);
        }

        private void replaceRegistration(ClassPath[] newPaths) {
            GlobalPathRegistry registry = GlobalPathRegistry.getDefault();

            ClassPath[] oldPaths = paths.getAndSet(newPaths);
            if (oldPaths != null) {
                registry.unregister(type, oldPaths);
            }
            if (newPaths != null) {
                registry.register(type, newPaths);
            }
        }

        public void register() {
            ClassPath[] newPaths = new ClassPath[]{cpProvider.getClassPaths(type)};
            replaceRegistration(newPaths);
        }

        public void unregister() {
            replaceRegistration(null);
        }
    }
}
