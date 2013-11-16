package org.netbeans.gradle.model;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectOutputStream;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.GradleProject;
import org.gradle.tooling.model.GradleTask;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.netbeans.gradle.model.api.GradleInfoQuery;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery;
import org.netbeans.gradle.model.internal.ModelQueryInput;
import org.netbeans.gradle.model.internal.ModelQueryOutput;
import org.netbeans.gradle.model.internal.ModelQueryOutputRef;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.SerializationUtils;
import org.netbeans.gradle.model.util.StringAsFileRef;

public final class GenericModelFetcher {
    private static final Charset INIT_SCRIPT_ENCODING = Charset.forName("UTF-8");
    private static final String INIT_SCRIPT_LOCATION = "/org/netbeans/gradle/scripts/dynamic-model-init-script.gradle";

    private static final AtomicReference<String> INIT_SCRIPT_REF = new AtomicReference<String>(null);

    private final Map<Object, GradleBuildInfoQuery<?>> buildInfoRequests;
    private final Map<Object, GradleProjectInfoQuery<?>> projectInfoRequests;
    private final Set<Class<?>> modelClasses;

    public GenericModelFetcher(
            Map<Object, GradleBuildInfoQuery<?>> buildInfoRequests,
            Map<Object, GradleProjectInfoQuery<?>> projectInfoRequests,
            Collection<Class<?>> modelClasses) {

        this.buildInfoRequests = CollectionUtils.copyNullSafeHashMap(buildInfoRequests);
        this.projectInfoRequests = CollectionUtils.copyNullSafeHashMap(projectInfoRequests);
        this.modelClasses = Collections.unmodifiableSet(new HashSet<Class<?>>(modelClasses));

        CollectionUtils.checkNoNullElements(this.modelClasses, "modelClasses");
    }

    private static void getJars(
            Collection<? extends GradleInfoQuery> queries,
            Collection<? super File> jars) {
        for (GradleInfoQuery query: queries) {
            Set<File> classPath = query.getInfoClassPath();
            jars.addAll(classPath);
        }
    }

    private Map<Object, BuildInfoBuilder<?>> getBuildInfoBuilders() {
        Map<Object, BuildInfoBuilder<?>> result
                = new HashMap<Object, BuildInfoBuilder<?>>(2 * buildInfoRequests.size());

        for (Map.Entry<Object, GradleBuildInfoQuery<?>> entry: buildInfoRequests.entrySet()) {
            BuildInfoBuilder<?> builder = entry.getValue().getInfoBuilder();
            result.put(entry.getKey(), builder);
        }
        return result;
    }

    private Map<Object, ProjectInfoBuilder<?>> getProjectInfoBuilders() {
        Map<Object, ProjectInfoBuilder<?>> result
                = new HashMap<Object, ProjectInfoBuilder<?>>(2 * projectInfoRequests.size());

        for (Map.Entry<Object, GradleProjectInfoQuery<?>> entry: projectInfoRequests.entrySet()) {
            ProjectInfoBuilder<?> builder = entry.getValue().getInfoBuilder();
            result.put(entry.getKey(), builder);
        }
        return result;
    }

    public FetchedModels getModels(ProjectConnection connection, OperationInitializer init) throws IOException {
        BuildActionExecuter<FetchedModels> executer
                = connection.action(new ModelFetcherBuildAction(getBuildInfoBuilders(), modelClasses));

        BuildOperationArgs buildOPArgs = new BuildOperationArgs();
        init.initOperation(buildOPArgs);
        buildOPArgs.setupLongRunningOP(executer);

        String[] userArgs = buildOPArgs.getArguments();
        if (userArgs == null) {
            userArgs = new String[0];
        }

        List<File> classPath = new LinkedList<File>();
        classPath.add(ClassLoaderUtils.getLocationOfClassPath());

        getJars(buildInfoRequests.values(), classPath);
        getJars(projectInfoRequests.values(), classPath);

        String initScript = getInitScript();
        initScript = initScript.replace("$MODEL_JAR_FILE_PATHS", toPastableFileList(classPath));

        ModelQueryInput modelInput = new ModelQueryInput(getProjectInfoBuilders());
        File modelInputFile = serializeToFile(modelInput);
        try {
            initScript = initScript.replace("$INPUT_FILE", toPastableString(modelInputFile));

            StringAsFileRef initScriptRef
                    = StringAsFileRef.createRef("dyn-model-gradle-init", initScript, INIT_SCRIPT_ENCODING);
            try {
                String[] executerArgs = new String[userArgs.length + 2];
                System.arraycopy(userArgs, 0, executerArgs, 0, userArgs.length);

                executerArgs[executerArgs.length - 2] = "--init-script";
                executerArgs[executerArgs.length - 1] = initScriptRef.getFile().getPath();

                executer.withArguments(executerArgs);

                return executer.run();
            } finally {
                initScriptRef.close();
            }
        } finally {
            modelInputFile.delete();
            // TODO: Log failure
        }
    }

    private static void serializeToFile(Object input, File outputFile) throws IOException {
        OutputStream fileOutput = new FileOutputStream(outputFile);
        try {
            ObjectOutputStream output = new ObjectOutputStream(fileOutput);
            try {
                output.writeObject(input);
            } finally {
                output.close();
            }
        } finally {
            fileOutput.close();
        }
    }

    private static File serializeToFile(Object input) throws IOException {
        File tmpFile = File.createTempFile("dyn-gradle-model", ".bin");
        try {
            serializeToFile(input, tmpFile);
            return tmpFile;
        } catch (Throwable ex) {
            if (!tmpFile.delete()) {
                IOException deleteEx = new IOException("Failed to remove temporary file: " + tmpFile);
                deleteEx.initCause(ex);
                throw deleteEx;
            }

            if (ex instanceof IOException) {
                throw (IOException)ex;
            }
            if (ex instanceof RuntimeException) {
                throw (RuntimeException)ex;
            }
            if (ex instanceof Error) {
                throw (Error)ex;
            }
            throw new RuntimeException(ex);
        }
    }

    private static String toPastableFileList(List<File> files) {
        StringBuilder result = new StringBuilder(1024);
        result.append('[');

        boolean first = true;
        for (File file: files) {
            if (first) {
                first = false;
            }
            else {
                result.append(", ");
            }
            result.append(toPastableString(file));
        }
        result.append(']');

        return result.toString();
    }

    private static String toPastableString(File file) {
        return toPastableString(file.getAbsolutePath());
    }

    private static String toPastableString(String value) {
        String result = value;
        result = result.replace("\\", "\\\\");
        result = result.replace("'", "\\'");
        result = result.replace("\\" + "u", "\\' + 'u");
        return "'" + result + "'";
    }

    private static String readAllFromReader(Reader reader) throws IOException {
        int expectedFileSize = 4 * 1024;

        StringBuilder result = new StringBuilder(expectedFileSize);
        char[] buf = new char[expectedFileSize];

        do {
            int readCount = reader.read(buf);
            if (readCount <= 0) {
                break;
            }

            result.append(buf, 0, readCount);
        } while (true);

        return result.toString();
    }

    private static String readResourceText(String resourcePath, Charset charset) throws IOException {
        URL resourceURL = GenericModelFetcher.class.getResource(resourcePath);
        if (resourceURL == null) {
            throw new IOException("Missing resource: " + resourcePath);
        }

        InputStream resourceIS = resourceURL.openStream();
        try {
            Reader resourceReader = new InputStreamReader(resourceIS, charset);
            try {
                return readAllFromReader(resourceReader);
            } finally {
                resourceReader.close();
            }
        } finally {
            resourceIS.close();
        }
    }

    private static String getInitScript() {
        String result = INIT_SCRIPT_REF.get();
        if (result == null) {
            try {
                result = readResourceText(INIT_SCRIPT_LOCATION, INIT_SCRIPT_ENCODING);
            } catch (IOException ex) {
                throw new IllegalStateException("Missing init-script file from resource.", ex);
            }
            INIT_SCRIPT_REF.set(result);
            result = INIT_SCRIPT_REF.get();
        }
        return result;
    }

    private static final class ModelFetcherBuildAction implements BuildAction<FetchedModels> {
        private static final long serialVersionUID = 1L;

        private final Map<Object, BuildInfoBuilder<?>> buildInfoRequests;
        private final Set<Class<?>> modelClasses;

        public ModelFetcherBuildAction(
                Map<Object, BuildInfoBuilder<?>> buildInfoRequests,
                Set<Class<?>> modelClasses) {
            this.buildInfoRequests = buildInfoRequests;
            this.modelClasses = modelClasses;
        }

        private ModelQueryOutput getModelOutput(ModelGetter getter) {
            byte[] serializedResult = getModel(getter, ModelQueryOutputRef.class)
                    .getSerializedModelQueryOutput();

            try {
                return (ModelQueryOutput)SerializationUtils.deserializeObject(serializedResult);
            } catch (ClassNotFoundException ex) {
                throw new RuntimeException(ex);
            }
        }

        private static <T> T getModel(ModelGetter getter, Class<T> modelClass) {
            T result = getter.findModel(modelClass);
            if (result == null) {
                throw new RuntimeException("Required model could not be loaded: " + modelClass);
            }
            return result;
        }

        private FetchedProjectModels getFetchedProjectModels(
                GradleProjectTree rootTree,
                Map<String, GradleProjectTree> projects,
                ModelGetter getter) {

            ModelQueryOutput modelOutput = getModelOutput(getter);
            GradleProjectTree projectTree = projects.get(modelOutput.getProjectFullName());
            if (projectTree == null) {
                // Shouldn't happen but try not to fail.
                EclipseProject eclipseProject = getModel(getter, EclipseProject.class);
                GradleProject gradleProject = eclipseProject.getGradleProject();

                GenericProjectProperties properties = new GenericProjectProperties(
                        gradleProject.getName(),
                        gradleProject.getPath(),
                        eclipseProject.getProjectDirectory());

                projectTree = new GradleProjectTree(
                        properties,
                        getTasksOfProjects(gradleProject),
                        Collections.<GradleProjectTree>emptyList());
            }

            Map<Class<?>, Object> toolingModels = new IdentityHashMap<Class<?>, Object>(2 * modelClasses.size());
            for (Class<?> modelClass: modelClasses) {
                Object modelValue = getter.findModel(modelClass);
                if (modelValue != null) {
                    toolingModels.put(modelClass, modelValue);
                }
            }

            return new FetchedProjectModels(
                    new GradleMultiProjectDef(rootTree, projectTree),
                    modelOutput.getProjectInfoResults(),
                    toolingModels);
        }

        private Collection<GradleTaskID> getTasksOfProjects(GradleProject project) {
            DomainObjectSet<? extends GradleTask> modelTasks = project.getTasks();
            List<GradleTaskID> result = new ArrayList<GradleTaskID>(modelTasks.size());
            for (GradleTask modelTask: modelTasks) {
                result.add(new GradleTaskID(modelTask.getName(), modelTask.getPath()));
            }
            return result;
        }

        private static GradleProject findAssociatedGradleProject(
                BasicGradleProject requiredProject,
                GradleProject projectTree) {
            String requiredPath = requiredProject.getPath();
            if (requiredPath.equals(projectTree.getPath())) {
                return projectTree;
            }

            return projectTree.findByPath(requiredPath);
        }

        // The reason why this method exists is because requesting
        // GradleProject for a particular BasicGradleProject returns the
        // root GradleProject instance (tested with 1.8 and 1.9).
        private GradleProject getGradleProjectForBasicProject(
                BuildController controller,
                BasicGradleProject project) {
            GradleProject gradleProject = controller.findModel(project, GradleProject.class);
            if (gradleProject == null) {
                return null;
            }

            return findAssociatedGradleProject(project, gradleProject);
        }

        private Collection<GradleTaskID> getTasksOfProjects(
                BuildController controller, BasicGradleProject project) {

            // TODO: Do not load tasks in later versions if the project is not
            //   evaluated.

            GradleProject gradleProject = getGradleProjectForBasicProject(controller, project);
            if (gradleProject == null) {
                return Collections.emptyList();
            }

            return getTasksOfProjects(gradleProject);
        }

        private GradleProjectTree parseTree(
                BuildController controller,
                BasicGradleProject basicProject,
                Map<String, GradleProjectTree> projects) {

            DomainObjectSet<? extends BasicGradleProject> modelChildren = basicProject.getChildren();
            List<GradleProjectTree> children = new ArrayList<GradleProjectTree>(modelChildren.size());

            for (BasicGradleProject modelChild: modelChildren) {
                children.add(parseTree(controller, modelChild, projects));
            }

            GenericProjectProperties properties = new GenericProjectProperties(
                    basicProject.getName(),
                    basicProject.getPath(),
                    basicProject.getProjectDirectory());

            Collection<GradleTaskID> tasks = getTasksOfProjects(controller, basicProject);

            GradleProjectTree result = new GradleProjectTree(properties, tasks, children);
            projects.put(properties.getProjectFullName(), result);
            return result;
        }

        public FetchedModels execute(final BuildController controller) {
            GradleBuild buildModel = controller.getBuildModel();

            Map<Object, Object> buildInfoResults = new HashMap<Object, Object>(2 * buildInfoRequests.size());
            for (Map.Entry<Object, BuildInfoBuilder<?>> entry: buildInfoRequests.entrySet()) {
                Object info = entry.getValue().getInfo(controller);
                if (info != null) {
                    buildInfoResults.put(entry.getKey(), info);
                }
            }

            Map<String, GradleProjectTree> projectTrees = new HashMap<String, GradleProjectTree>(64);
            GradleProjectTree rootTree = parseTree(controller, buildModel.getRootProject(), projectTrees);

            FetchedProjectModels defaultProjectModels = getFetchedProjectModels(rootTree, projectTrees, new ModelGetter() {
                public <T> T findModel(Class<T> modelClass) {
                    return controller.findModel(modelClass);
                }
            });

            List<FetchedProjectModels> otherModels = new LinkedList<FetchedProjectModels>();
            for (final BasicGradleProject projectRef: buildModel.getProjects()) {
                FetchedProjectModels otherModel = getFetchedProjectModels(rootTree, projectTrees, new ModelGetter() {
                    public <T> T findModel(Class<T> modelClass) {
                        return controller.findModel(projectRef, modelClass);
                    }
                });

                otherModels.add(otherModel);
            }

            FetchedBuildModels buildModels = new FetchedBuildModels(buildInfoResults);
            return new FetchedModels(buildModels, defaultProjectModels, otherModels);
        }
    }

    private interface ModelGetter {
        public <T> T findModel(Class<T> modelClass);
    }
}
