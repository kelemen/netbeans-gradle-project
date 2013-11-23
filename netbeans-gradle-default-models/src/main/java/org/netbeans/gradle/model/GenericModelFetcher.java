package org.netbeans.gradle.model;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
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
import org.netbeans.gradle.model.internal.CustomSerializedMap;
import org.netbeans.gradle.model.internal.ModelQueryInput;
import org.netbeans.gradle.model.internal.ModelQueryOutput;
import org.netbeans.gradle.model.internal.ModelQueryOutputRef;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.SerializationUtils;
import org.netbeans.gradle.model.util.TemporaryFileManager;
import org.netbeans.gradle.model.util.TemporaryFileRef;

public final class GenericModelFetcher {
    private static final Charset INIT_SCRIPT_ENCODING = Charset.forName("UTF-8");
    private static final String INIT_SCRIPT_LOCATION = "/org/netbeans/gradle/scripts/dynamic-model-init-script.gradle";

    private static final AtomicReference<String> INIT_SCRIPT_REF = new AtomicReference<String>(null);

    // key -> list of BuildInfoBuilder
    private final GradleInfoQueryMap buildInfoBuilders;

    // key -> list of ProjectInfoBuilder
    private final GradleInfoQueryMap projectInfoBuilders;

    // TODO: These classes must be key based as well.
    private final Set<Class<?>> modelClasses;

    public GenericModelFetcher(
            Map<Object, List<GradleBuildInfoQuery<?>>> buildInfoRequests,
            Map<Object, List<GradleProjectInfoQuery<?>>> projectInfoRequests,
            Collection<Class<?>> modelClasses) {

        this.buildInfoBuilders = GradleInfoQueryMap.fromBuildInfos(buildInfoRequests);
        this.projectInfoBuilders = GradleInfoQueryMap.fromProjectInfos(projectInfoRequests);
        this.modelClasses = Collections.unmodifiableSet(new HashSet<Class<?>>(modelClasses));

        CollectionUtils.checkNoNullElements(this.modelClasses, "modelClasses");
    }

    private static void getJars(
            Collection<? extends GradleInfoQuery> queries,
            Collection<? super File> jars) {
        for (GradleInfoQuery query: queries) {
            Set<File> classPath = query.getInfoClassPath().getJarFiles();
            jars.addAll(classPath);
        }
    }

    private FetchedProjectModels transformActionModels(ActionFetchedProjectModels actionModels) {
        GradleMultiProjectDef projectDef = actionModels.getProjectDef();
        Map<Class<?>, Object> toolingModels = actionModels.getToolingModels();
        Map<Object, List<?>> projectInfoResults
                = projectInfoBuilders.deserializeResults(actionModels.getProjectInfoResults());

        return new FetchedProjectModels(projectDef, projectInfoResults, toolingModels);
    }

    private Collection<FetchedProjectModels> transformActionModels(Collection<ActionFetchedProjectModels> actionModels) {
        List<FetchedProjectModels> result = new ArrayList<FetchedProjectModels>(actionModels.size());
        for (ActionFetchedProjectModels entry: actionModels) {
            result.add(transformActionModels(entry));
        }
        return result;
    }

    private FetchedModels transformActionModels(ActionFetchedModels actionModels) {
        Map<Object, List<?>> buildModels
                = buildInfoBuilders.deserializeResults(actionModels.getBuildModels());
        FetchedProjectModels defaultProjectModels
                = transformActionModels(actionModels.getDefaultProjectModels());
        Collection<FetchedProjectModels> otherProjectModels
                = transformActionModels(actionModels.getOtherProjectModels());

        return new FetchedModels(new FetchedBuildModels(buildModels), defaultProjectModels, otherProjectModels);
    }

    public FetchedModels getModels(ProjectConnection connection, OperationInitializer init) throws IOException {
        BuildActionExecuter<ActionFetchedModels> executer = connection.action(new ModelFetcherBuildAction(
                buildInfoBuilders,
                modelClasses));

        BuildOperationArgs buildOPArgs = new BuildOperationArgs();
        init.initOperation(buildOPArgs);
        buildOPArgs.setupLongRunningOP(executer);

        String[] userArgs = buildOPArgs.getArguments();
        if (userArgs == null) {
            userArgs = new String[0];
        }

        String initScript = getInitScript();

        initScript = initScript.replace(
                "$NB_BOOT_CLASSPATH",
                toPastableString(ClassLoaderUtils.getLocationOfClassPath().getPath()));

        TemporaryFileManager fileManager = TemporaryFileManager.getDefault();

        ModelQueryInput modelInput = new ModelQueryInput(projectInfoBuilders.getSerializableBuilderMap());
        TemporaryFileRef modelInputFile = fileManager.createFileFromSerialized("model-input", modelInput);
        try {
            initScript = initScript.replace("$INPUT_FILE", toPastableString(modelInputFile.getFile()));

            TemporaryFileRef initScriptRef = fileManager
                    .createFile("dyn-model-gradle-init", initScript, INIT_SCRIPT_ENCODING);
            try {
                String[] executerArgs = new String[userArgs.length + 2];
                System.arraycopy(userArgs, 0, executerArgs, 0, userArgs.length);

                executerArgs[executerArgs.length - 2] = "--init-script";
                executerArgs[executerArgs.length - 1] = initScriptRef.getFile().getPath();

                executer.withArguments(executerArgs);

                return transformActionModels(executer.run());
            } finally {
                initScriptRef.close();
            }
        } finally {
            modelInputFile.close();
        }
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

    private static final class ModelFetcherBuildAction implements BuildAction<ActionFetchedModels> {
        private static final long serialVersionUID = 1L;

        // key -> list of BuildInfoBuilder
        private final CustomSerializedMap.Deserializer serializedBuildInfoRequests;
        private final Set<Class<?>> modelClasses;

        public ModelFetcherBuildAction(
                GradleInfoQueryMap buildInfoRequests,
                Set<Class<?>> modelClasses) {
            this.serializedBuildInfoRequests = buildInfoRequests.getSerializableBuilderMap();
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

        private ActionFetchedProjectModels getFetchedProjectModels(
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

            return new ActionFetchedProjectModels(
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

        public ActionFetchedModels execute(final BuildController controller) {
            GradleBuild buildModel = controller.getBuildModel();

            ClassLoader parentClassLoader = getClass().getClassLoader();
            Map<Object, List<?>> buildInfoRequests = serializedBuildInfoRequests.deserialize(parentClassLoader);

            CustomSerializedMap.Builder buildInfoResults = new CustomSerializedMap.Builder(buildInfoRequests.size());
            for (Map.Entry<Object, List<?>> entry: buildInfoRequests.entrySet()) {
                Object key = entry.getKey();

                for (Object builder: entry.getValue()) {
                    Object info = ((BuildInfoBuilder<?>)builder).getInfo(controller);
                    if (info != null) {
                        buildInfoResults.addValue(key, info);
                    }
                }
            }

            Map<String, GradleProjectTree> projectTrees = new HashMap<String, GradleProjectTree>(64);
            GradleProjectTree rootTree = parseTree(controller, buildModel.getRootProject(), projectTrees);

            ActionFetchedProjectModels defaultProjectModels = getFetchedProjectModels(rootTree, projectTrees, new ModelGetter() {
                public <T> T findModel(Class<T> modelClass) {
                    return controller.findModel(modelClass);
                }
            });

            List<ActionFetchedProjectModels> otherModels = new LinkedList<ActionFetchedProjectModels>();
            for (final BasicGradleProject projectRef: buildModel.getProjects()) {
                ActionFetchedProjectModels otherModel = getFetchedProjectModels(rootTree, projectTrees, new ModelGetter() {
                    public <T> T findModel(Class<T> modelClass) {
                        return controller.findModel(projectRef, modelClass);
                    }
                });

                otherModels.add(otherModel);
            }

            CustomSerializedMap buildModels = buildInfoResults.create();
            return new ActionFetchedModels(buildModels, defaultProjectModels, otherModels);
        }
    }

    private interface ModelGetter {
        public <T> T findModel(Class<T> modelClass);
    }
}
