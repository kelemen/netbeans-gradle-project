package org.netbeans.gradle.project.model;

import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.gradle.tooling.BuildException;
import org.gradle.tooling.GradleConnectionException;
import org.gradle.tooling.GradleConnector;
import org.gradle.tooling.ModelBuilder;
import org.gradle.tooling.ProgressEvent;
import org.gradle.tooling.ProgressListener;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.UnknownModelException;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.Model;
import org.gradle.tooling.model.idea.IdeaContentRoot;
import org.gradle.tooling.model.idea.IdeaModule;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.model.NbSourceRoot;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.tasks.DaemonTask;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.netbeans.gradle.project.tasks.GradleTasks;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.RequestProcessor;
import org.openide.util.lookup.Lookups;

public final class GradleModelLoader {
    private static final Logger LOGGER = Logger.getLogger(GradleModelLoader.class.getName());

    private static final RequestProcessor PROJECT_LOADER
            = new RequestProcessor("Gradle-Project-Loader", 1, true);

    private static GradleModelCache CACHE = new GradleModelCache(100);
    private static ModelLoadSupport LISTENERS = new ModelLoadSupport();

    static {
        GlobalGradleSettings.getProjectCacheSize().addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                CACHE.setMaxCapacity(GlobalGradleSettings.getProjectCacheSize().getValue());
            }
        });
    }

    public static void addModelLoadedListener(ModelLoadListener listener) {
        LISTENERS.addListener(listener);
    }

    public static void removeModelLoadedListener(ModelLoadListener listener) {
        LISTENERS.removeListener(listener);
    }

    public static GradleConnector createGradleConnector(final NbGradleProject project) {
        final GradleConnector result = GradleConnector.newConnector();

        File gradleUserHome = GlobalGradleSettings.getGradleUserHomeDir().getValue();
        if (gradleUserHome != null) {
            result.useGradleUserHomeDir(gradleUserHome);
        }

        GradleLocation gradleLocation = project.getProperties().getGradleLocation().getValue();

        gradleLocation.applyLocation(new GradleLocation.Applier() {
            @Override
            public void applyVersion(String versionStr) {
                result.useGradleVersion(versionStr);
            }

            @Override
            public void applyDirectory(File gradleHome) {
                result.useInstallation(gradleHome);
            }

            @Override
            public void applyDistribution(URI location) {
                result.useDistribution(location);
            }

            @Override
            public void applyDefault() {
            }
        });

        return result;
    }

    private static NbGradleModel tryGetFromCache(File projectDir) {
        File settingsFile = NbGradleModel.findSettingsGradle(projectDir);
        if (settingsFile == null) {
            LOGGER.log(Level.WARNING, "Settings file of the project disappeared: {0}", settingsFile);
            return null;
        }

        NbGradleModel result = projectDir != null
                ? CACHE.tryGet(projectDir, settingsFile)
                : null;

        if (result != null && result.isDirty()) {
            result = null;
        }
        return result;
    }

    public static void fetchModel(
            final NbGradleProject project,
            final ModelRetrievedListener listener) {
        fetchModel(project, false, listener);
    }

    public static void fetchModel(
            final NbGradleProject project,
            final boolean mayFetchFromCache,
            final ModelRetrievedListener listener) {
        if (project == null) throw new NullPointerException("project");
        if (listener == null) throw new NullPointerException("listener");

        final File projectDir = project.getProjectDirectoryAsFile();
        String caption = NbStrings.getLoadingProjectText(project.getDisplayName());
        GradleDaemonManager.submitGradleTask(PROJECT_LOADER, caption, new DaemonTask() {
            @Override
            public void run(ProgressHandle progress) {
                NbGradleModel model = null;
                Throwable error = null;
                try {
                    if (mayFetchFromCache) {
                        model = tryGetFromCache(projectDir);
                    }
                    if (model == null) {
                        model = loadModelWithProgress(project, progress);
                    }
                } catch (IOException ex) {
                    error = ex;
                } catch (BuildException ex) {
                    error = ex;
                } catch (GradleConnectionException ex) {
                    error = ex;
                } finally {
                    listener.onComplete(model, error);
                }
            }
        }, true, GradleTasks.projectTaskCompleteListener(project));
    }

    public static File getScriptJavaHome(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        JavaPlatform platform = project.getProperties().getScriptPlatform().getValue();
        FileObject jdkHomeObj = platform != null
                ? GlobalGradleSettings.getHomeFolder(platform)
                : null;
        return jdkHomeObj != null ? FileUtil.toFile(jdkHomeObj) : null;
    }

    private static Object getRawModelWithProgress(
            NbGradleProject project,
            final ProgressHandle progress,
            ProjectConnection projectConnection,
            Class<?> model) {
        // Actually this cast is only needed to be complile against pre 1.6
        // Tooling API because from version 1.6 there is no need to extend
        // Model.
        // TODO: Remove this hack after upgrading to 1.6
        @SuppressWarnings("unchecked")
        Class<? extends Model> castedModel = (Class<? extends Model>)model;
        return getModelWithProgress(project, progress, projectConnection, castedModel);
    }

    private static <T extends Model> T getModelWithProgress(
            NbGradleProject project,
            final ProgressHandle progress,
            ProjectConnection projectConnection,
            Class<? extends T> model) {
        ModelBuilder<? extends T> builder = projectConnection.model(model);

        File jdkHome = getScriptJavaHome(project);
        if (jdkHome != null && !jdkHome.getPath().isEmpty()) {
            builder.setJavaHome(jdkHome);
        }

        List<String> globalJvmArgs = GlobalGradleSettings.getGradleJvmArgs().getValue();

        if (globalJvmArgs != null && !globalJvmArgs.isEmpty()) {
            builder.setJvmArguments(globalJvmArgs.toArray(new String[0]));
        }

        builder.addProgressListener(new ProgressListener() {
            @Override
            public void statusChanged(ProgressEvent pe) {
                progress.progress(pe.getDescription());
            }
        });

        return builder.get();
    }

    public static File tryGetModuleDir(IdeaModule module) {
        DomainObjectSet<? extends IdeaContentRoot> contentRoots = module.getContentRoots();
        return contentRoots.isEmpty() ? null : contentRoots.getAt(0).getRootDirectory();
    }

    public static IdeaModule tryFindMainModule(File projectDir, IdeaProject ideaModel) {
        for (IdeaModule module: ideaModel.getModules()) {
            File moduleDir = tryGetModuleDir(module);
            if (moduleDir != null && moduleDir.equals(projectDir)) {
                return module;
            }
        }
        return null;
    }

    public static void introduceLoadedModel(NbGradleModel model) {
        CACHE.addToCache(model);
        LISTENERS.fireEvent(model);
    }

    private static Lookup getExtensionModels(
            NbGradleProject project,
            ProgressHandle progress,
            ProjectConnection projectConnection) {

        Set<Class<?>> loadedClasses = Collections.newSetFromMap(new IdentityHashMap<Class<?>, Boolean>());
        List<Object> models = new LinkedList<Object>();
        for (GradleProjectExtension extension: project.getExtensions()) {
            for (List<Class<?>> modelRequest: extension.getGradleModels()) {
                for (Class<?> modelClass: modelRequest) {
                    try {
                        if (!loadedClasses.contains(modelClass)) {
                            loadedClasses.add(modelClass);

                            Object rawModel = getRawModelWithProgress(
                                    project, progress, projectConnection, modelClass);
                            models.add(rawModel);
                        }
                        break;
                    } catch (UnknownModelException ex) {
                        Throwable loggedException = LOGGER.isLoggable(Level.FINE)
                                ? ex
                                : null;
                        LOGGER.log(Level.INFO, "Cannot find model " + modelClass.getName(), loggedException);
                    }
                }
            }
        }

        JavaExtension javaExt = project.lookupExtension(JavaExtension.class);
        if (javaExt != null) {
            models.add(javaExt.addToModelLookup(Lookups.fixed(models.toArray())));
        }

        return Lookups.fixed(models.toArray());
    }

    private static IdeaModule tryFindIdeaModule(NbGradleProject project, Lookup lookup) {
        IdeaProject ideaProject = lookup.lookup(IdeaProject.class);
        if (ideaProject != null) {
            File projectDir = project.getProjectDirectoryAsFile();
            return tryFindMainModule(projectDir, ideaProject);
        }
        else {
            return null;
        }
    }

    public static List<IdeaModule> getChildModules(IdeaModule module) {
        Collection<? extends GradleProject> children = module.getGradleProject().getChildren();
        Set<String> childrenPaths = new HashSet<String>(2 * children.size());
        for (GradleProject child: children) {
            childrenPaths.add(child.getPath());
        }

        List<IdeaModule> result = new LinkedList<IdeaModule>();
        for (IdeaModule candidateChild: module.getProject().getModules()) {
            if (childrenPaths.contains(candidateChild.getGradleProject().getPath())) {
                result.add(candidateChild);
            }
        }
        return result;
    }


    private static GradleProjectInfo tryCreateProjectTreeFromIdea(IdeaModule module) {
        DomainObjectSet<? extends IdeaContentRoot> contentRoots = module.getContentRoots();
        if (contentRoots.isEmpty()) {
            return null;
        }

        File moduleDir = contentRoots.iterator().next().getRootDirectory();

        int expectedChildCount = module.getGradleProject().getChildren().size();
        List<GradleProjectInfo> children = new ArrayList<GradleProjectInfo>(expectedChildCount);
        for (IdeaModule child: getChildModules(module)) {
            GradleProjectInfo childInfo = tryCreateProjectTreeFromIdea(child);
            if (childInfo != null) {
                children.add(childInfo);
            }
        }

        return new GradleProjectInfo(module.getGradleProject(), moduleDir, children);
    }

    private static NbGradleModel loadModelWithProgress(
            NbGradleProject project,
            ProgressHandle progress) throws IOException {
        File projectDir = project.getProjectDirectoryAsFile();

        LOGGER.log(Level.INFO, "Loading Gradle project from directory: {0}", projectDir);

        IdeaModule ideaModule = null;
        Lookup extensionModels;

        GradleConnector gradleConnector = createGradleConnector(project);
        gradleConnector.forProjectDirectory(projectDir);
        ProjectConnection projectConnection = null;
        try {
            projectConnection = gradleConnector.connect();

            extensionModels = getExtensionModels(project, progress, projectConnection);

            ideaModule = tryFindIdeaModule(project, extensionModels);
        } finally {
            if (projectConnection != null) {
                projectConnection.close();
            }
        }

        progress.progress(NbStrings.getParsingModel());
        if (ideaModule != null) {
            GradleProjectInfo projectInfo = tryCreateProjectTreeFromIdea(ideaModule);
            if (projectInfo != null) {
                NbGradleModel result = new NbGradleModel(projectInfo, projectDir, extensionModels);
                introduceLoadedModel(result);
                return result;
            }

            LOGGER.log(Level.WARNING, "Failed to create project info for project: {0}", projectDir);
        }

        return createEmptyModel(projectDir, extensionModels);
    }

    public static NbGradleModel createEmptyModel(File projectDir) {
        return createEmptyModel(projectDir, Lookup.EMPTY);
    }

    public static NbGradleModel createEmptyModel(File projectDir, Lookup extensionModels) {
        return new NbGradleModel(GradleProjectInfo.createEmpty(projectDir), projectDir, extensionModels);
    }

    private static <K, V> void addToMap(Map<K, List<V>> map, K key, V value) {
        List<V> valueList = map.get(key);
        if (valueList == null) {
            valueList = new LinkedList<V>();
            map.put(key, valueList);
        }
        valueList.add(value);
    }

    public static List<NbSourceRoot> nameSourceRoots(List<File> files) {
        // The common case
        if (files.size() == 1) {
            File file = files.get(0);
            return Collections.singletonList(new NbSourceRoot(file, file.getName()));
        }

        Map<String, List<FileWithBase>> nameToFile
                = new HashMap<String, List<FileWithBase>>(files.size() * 2 + 1);

        int fileIndex = 0;
        for (File file: files) {
            String name = file.getName();
            File parent = file.getParentFile();
            addToMap(nameToFile, name, new FileWithBase(fileIndex, parent, file));
            fileIndex++;
        }

        boolean didSomething;
        do {
            didSomething = false;

            List<Map.Entry<String, List<FileWithBase>>> currentEntries
                    = new ArrayList<Map.Entry<String, List<FileWithBase>>>(nameToFile.entrySet());
            for (Map.Entry<String, List<FileWithBase>> entry: currentEntries) {
                String entryName = entry.getKey();
                List<FileWithBase> entryFiles = entry.getValue();

                int renameableCount = 0;
                for (FileWithBase file: entryFiles) {
                    if (file.base != null) renameableCount++;
                }

                if (renameableCount > 1) {
                    nameToFile.remove(entryName);
                    for (FileWithBase file: entryFiles) {
                        if (file.base != null) {
                            String newName = file.base.getName() + '/' + entryName;
                            File newParent = file.base.getParentFile();
                            addToMap(nameToFile,
                                    newName,
                                    new FileWithBase(file.index, newParent, file.file));
                        }
                        else {
                            addToMap(nameToFile, entryName, file);
                        }
                    }
                    didSomething = true;
                }
            }
        } while (didSomething);

        NbSourceRoot[] result = new NbSourceRoot[fileIndex];
        for (Map.Entry<String, List<FileWithBase>> entry: nameToFile.entrySet()) {
            String entryName = entry.getKey();
            for (FileWithBase file: entry.getValue()) {
                result[file.index] = new NbSourceRoot(file.file, entryName);
            }
        }

        return Arrays.asList(result);
    }

    private static final class FileWithBase {
        public final int index;
        public final File base;
        public final File file;

        public FileWithBase(int index, File base, File file) {
            assert file != null;

            this.index = index;
            this.base = base;
            this.file = file;
        }
    }

    private GradleModelLoader() {
        throw new AssertionError();
    }
}
