package org.netbeans.gradle.project.java;

import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
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
import org.netbeans.gradle.project.java.query.JavaInitScriptQuery;
import org.netbeans.gradle.project.java.query.JavaProjectContextActions;
import org.netbeans.gradle.project.java.tasks.GradleJavaBuiltInCommands;
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
    private final File projectDirectoryAsFile;
    private volatile NbJavaModel currentModel;

    private final GradleClassPathProvider cpProvider;

    private final AtomicReference<Lookup> lookupRef;
    private final AtomicReference<Lookup> permanentLookupRef;

    private final DynamicLookup extensionLookup;
    private final Lookup protectedExtensionLookup;

    private JavaExtension(Project project) throws IOException {
        if (project == null) throw new NullPointerException("project");

        this.projectDirectoryAsFile = FileUtil.toFile(project.getProjectDirectory());
        if (projectDirectoryAsFile == null) throw new NullPointerException("projectDirAsFile");

        this.project = project;
        this.currentModel = NbJavaModelUtils.createEmptyModel(project.getProjectDirectory());
        this.cpProvider = new GradleClassPathProvider(this);
        this.lookupRef = new AtomicReference<Lookup>(null);
        this.permanentLookupRef = new AtomicReference<Lookup>(null);
        this.extensionLookup = new DynamicLookup();
        this.protectedExtensionLookup = DynamicLookup.viewLookup(extensionLookup);
    }

    @Override
    public String getExtensionName() {
        // Do not return JavaExtension.class.getName() because this string must
        // remain the same even if this class is renamed.
        return "org.netbeans.gradle.project.java.JavaExtension";
    }

    public static JavaExtension create(Project project) throws IOException {
        JavaExtension result = new JavaExtension(project);
        result.updateLookup(false);
        return result;
    }

    public boolean isOwnerProject(File file) {
        FileObject fileObj;

        File currentFile = file;
        fileObj = FileUtil.toFileObject(currentFile);
        while (fileObj == null) {
            currentFile = currentFile.getParentFile();
            if (currentFile == null) {
                return false;
            }

            fileObj = FileUtil.toFileObject(currentFile);
        }

        return isOwnerProject(fileObj);
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

    private void initLookup(Lookup lookup) {
        for (ProjectInitListener listener: lookup.lookupAll(ProjectInitListener.class)) {
            listener.onInitProject();
        }
    }

    // These classes are on the lookup always.
    private Lookup getPermanentLookup() {
        Lookup lookup = permanentLookupRef.get();
        if (lookup == null) {
            lookup = Lookups.singleton(new OpenHook());

            if (permanentLookupRef.compareAndSet(null, lookup)) {
                initLookup(lookup);
            }
            lookup = permanentLookupRef.get();
        }
        return lookup;
    }

    private Lookup getStaticLookup() {
        Lookup lookup = lookupRef.get();
        if (lookup == null) {
            lookup = Lookups.fixed(
                    new GradleProjectSources(this),
                    cpProvider,
                    new GradleSourceLevelQueryImplementation(this),
                    new GradleUnitTestFinder(this),
                    new GradleAnnotationProcessingQuery(),
                    new GradleSourceForBinaryQuery(this),
                    new GradleBinaryForSourceQuery(this),
                    new GradleProjectTemplates(),
                    new JavaExtensionNodes(this),
                    new JavaProjectContextActions(this),
                    new GradleJavaBuiltInCommands(this),
                    new JavaInitScriptQuery(),
                    new J2SEPlatformFromScriptQueryImpl(this) // internal use only
                    );

            if (lookupRef.compareAndSet(null, lookup)) {
                initLookup(lookup);
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

    public File getProjectDirectoryAsFile() {
        return projectDirectoryAsFile;
    }

    public String getName() {
        return currentModel.getMainModule().getShortName();
    }

    /**
     * This method is a pure hack to prevent regression when no extension is
     * used other than the standard Java project. That is, this method is called
     * right when models are loaded and will add other projects to the cache
     * if only the JavaExtension is present.
     */
    public List<?> addToModelLookup(Lookup modelLookup) {
        NbJavaModel result = null;
        IdeaProject ideaProject = modelLookup.lookup(IdeaProject.class);
        if (ideaProject != null) {
            try {
                result = NbJavaModelUtils.parseFromIdeaModel(
                        currentModel.getMainModule().getModuleDir(),
                        ideaProject);
            } catch (IOException ex) {
                LOGGER.log(Level.FINE, "Failed to parse model.", ex);
                // We will most likely log this exception again in modelsLoaded.
            }
        }

        return result != null
                ? Collections.singletonList(result)
                : Collections.emptyList();
    }

    private void switchToEmptyModel() {
        currentModel = NbJavaModelUtils.createEmptyModel(currentModel.getMainModule().getModuleDir());
    }

    private void updateLookup(boolean loaded) {
        if (loaded) {
            extensionLookup.replaceLookups(getPermanentLookup(), getStaticLookup());
        }
        else {
            extensionLookup.replaceLookups(getPermanentLookup());
        }
    }

    @Override
    public Set<String> modelsLoaded(Lookup modelLookup) {
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

        updateLookup(loaded);

        for (JavaModelChangeListener listener: getExtensionLookup().lookupAll(JavaModelChangeListener.class)) {
            listener.onModelChange();
        }

        return Collections.emptySet();
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
