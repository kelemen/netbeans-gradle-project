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
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.gradle.tooling.BuildAction;
import org.gradle.tooling.BuildActionExecuter;
import org.gradle.tooling.BuildController;
import org.gradle.tooling.ProjectConnection;
import org.gradle.tooling.model.DomainObjectSet;
import org.gradle.tooling.model.gradle.BasicGradleProject;
import org.gradle.tooling.model.gradle.GradleBuild;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery2;
import org.netbeans.gradle.model.internal.CustomSerializedMap;
import org.netbeans.gradle.model.internal.ModelQueryInput;
import org.netbeans.gradle.model.internal.ModelQueryOutput;
import org.netbeans.gradle.model.internal.ModelQueryOutputRef;
import org.netbeans.gradle.model.util.BasicFileUtils;
import org.netbeans.gradle.model.util.BuilderUtils;
import org.netbeans.gradle.model.util.ClassLoaderUtils;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.SerializationCaches;
import org.netbeans.gradle.model.util.SerializationUtils;
import org.netbeans.gradle.model.util.TemporaryFileManager;
import org.netbeans.gradle.model.util.TemporaryFileRef;

public final class GenericModelFetcher {
    private static final Charset INIT_SCRIPT_ENCODING = Charset.forName("UTF-8");
    private static final String INIT_SCRIPT_LOCATION = "/org/netbeans/gradle/scripts/dynamic-model-init-script.gradle";

    private static final AtomicReference<String> INIT_SCRIPT_REF = new AtomicReference<String>(null);

    private static final String DEFAULT_MODEL_INPUT_PREFIX = "model-input";
    private static final String DEFAULT_INIT_SCRIPT_PREFIX = "dyn-model-gradle-init";

    private static volatile String modelInputPrefix = DEFAULT_MODEL_INPUT_PREFIX;
    private static volatile String initScriptPrefix = DEFAULT_INIT_SCRIPT_PREFIX;

    // key -> list of BuildInfoBuilder
    private final GradleInfoQueryMap buildInfoBuilders;

    // key -> list of ProjectInfoBuilder
    private final GradleInfoQueryMap projectInfoBuilders;

    // TODO: These classes must be key based as well.
    private final Set<Class<?>> modelClasses;

    public GenericModelFetcher(
            Map<Object, List<GradleBuildInfoQuery<?>>> buildInfoRequests,
            Map<Object, List<GradleProjectInfoQuery2<?>>> projectInfoRequests,
            Collection<Class<?>> modelClasses) {

        this.buildInfoBuilders = GradleInfoQueryMap.fromBuildInfos(buildInfoRequests);
        this.projectInfoBuilders = GradleInfoQueryMap.fromProjectInfos(projectInfoRequests);
        this.modelClasses = Collections.unmodifiableSet(new HashSet<Class<?>>(modelClasses));

        CollectionUtils.checkNoNullElements(this.modelClasses, "modelClasses");
    }

    public static void setDefaultPrefixes() {
        modelInputPrefix = DEFAULT_MODEL_INPUT_PREFIX;
        initScriptPrefix = DEFAULT_INIT_SCRIPT_PREFIX;
    }

    public static void setModelInputPrefix(String modelInputPrefix) {
        if (modelInputPrefix == null) throw new NullPointerException("modelInputPrefix");
        GenericModelFetcher.modelInputPrefix = modelInputPrefix;
    }

    public static void setInitScriptPrefix(String initScriptPrefix) {
        if (initScriptPrefix == null) throw new NullPointerException("initScriptPrefix");
        GenericModelFetcher.initScriptPrefix = initScriptPrefix;
    }

    private FetchedModelsOrError transformActionModels(ActionFetchedModelsOrError actionModels) {
        return new FetchedModelsOrError(
                transformActionModels(actionModels.getModels()),
                actionModels.getBuildScriptEvaluationError(),
                actionModels.getUnexpectedError());
    }

    private FetchedProjectModels transformActionModels(ActionFetchedProjectModels actionModels) {
        GradleMultiProjectDef projectDef = actionModels.getProjectDef();
        Map<Class<?>, Object> toolingModels = actionModels.getToolingModels();
        Map<Object, List<?>> projectInfoResults = projectInfoBuilders.deserializeResults(
                actionModels.getProjectInfoResults(),
                GradleInfoQueryMap.builderIssueTransformer());
        Throwable issue = actionModels.getIssue();

        return new FetchedProjectModels(projectDef, projectInfoResults, toolingModels, issue);
    }

    private Collection<FetchedProjectModels> transformActionModels(Collection<ActionFetchedProjectModels> actionModels) {
        List<FetchedProjectModels> result = new ArrayList<FetchedProjectModels>(actionModels.size());
        for (ActionFetchedProjectModels entry: actionModels) {
            result.add(transformActionModels(entry));
        }
        return result;
    }

    private FetchedModels transformActionModels(ActionFetchedModels actionModels) {
        if (actionModels == null) {
            return null;
        }

        Map<Object, List<?>> buildModels = buildInfoBuilders.deserializeResults(
                actionModels.getBuildModels(),
                GradleInfoQueryMap.builderIssueTransformer());
        FetchedProjectModels defaultProjectModels
                = transformActionModels(actionModels.getDefaultProjectModels());
        Collection<FetchedProjectModels> otherProjectModels
                = transformActionModels(actionModels.getOtherProjectModels());

        return new FetchedModels(new FetchedBuildModels(buildModels), defaultProjectModels, otherProjectModels);
    }

    public FetchedModelsOrError getModels(ProjectConnection connection, OperationInitializer init) throws IOException {
        BuildActionExecuter<ActionFetchedModelsOrError> executer = connection.action(
                new ModelFetcherBuildAction(buildInfoBuilders, modelClasses));

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
                toPastableString(ClassLoaderUtils.getUrlOfClassPath().toExternalForm()));

        TemporaryFileManager fileManager = TemporaryFileManager.getDefault();

        ModelQueryInput modelInput = new ModelQueryInput(projectInfoBuilders.getSerializableBuilderMap());
        TemporaryFileRef modelInputFile = fileManager.createFileFromSerialized(modelInputPrefix, modelInput);
        try {
            initScript = initScript.replace("$INPUT_FILE", toPastableString(modelInputFile.getFile()));

            TemporaryFileRef initScriptRef = fileManager
                    .createFile(initScriptPrefix, initScript, INIT_SCRIPT_ENCODING);
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
        result = BasicFileUtils.toSafelyPastableToJavaCode(result);
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

    private static <T> T getModel(ModelGetter getter, Class<T> modelClass) {
        T result = getter.findModel(modelClass);
        if (result == null) {
            throw new RuntimeException("Required model could not be loaded: " + modelClass);
        }
        return result;
    }

    private static ModelGetter defaultModelGetter(final BuildController controller) {
        return new ModelGetter() {
            @Override
            public <T> T findModel(Class<T> modelClass) {
                return controller.getModel(modelClass);
            }
        };
    }

    private static ModelGetter projectModelGetter(
            final BuildController controller,
            final BasicGradleProject referenceProject) {

        return new ModelGetter() {
            @Override
            public <T> T findModel(Class<T> modelClass) {
                return controller.findModel(referenceProject, modelClass);
            }
        };
    }

    private static ModelQueryOutput getModelOutput(SerializationCache cache, ModelGetter getter) {
        byte[] serializedResult = getModel(getter, ModelQueryOutputRef.class)
                .getSerializedModelQueryOutput();

        try {
            return (ModelQueryOutput)SerializationUtils.deserializeObject(serializedResult, cache);
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static final class ModelFetcherBuildAction implements BuildAction<ActionFetchedModelsOrError> {
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

        private CustomSerializedMap getBuildInfoResults(BuildController controller) {
            ClassLoader parentClassLoader = getClass().getClassLoader();
            Map<Object, List<?>> buildInfoRequests = serializedBuildInfoRequests.deserialize(SerializationCaches.getDefault(),
                    parentClassLoader,
                    GradleInfoQueryMap.buildInfoBuilderIssueTransformer());

            if (buildInfoRequests.isEmpty()) {
                return CustomSerializedMap.EMPTY;
            }

            CustomSerializedMap.Builder result = new CustomSerializedMap.Builder(buildInfoRequests.size());
            for (Map.Entry<Object, List<?>> entry: buildInfoRequests.entrySet()) {
                Object key = entry.getKey();

                for (Object buildBuilder: entry.getValue()) {
                    Object info = null;
                    Throwable issue = null;
                    BuildInfoBuilder<?> builder = null;

                    try {
                        builder = (BuildInfoBuilder<?>)buildBuilder;
                        info = builder.getInfo(controller);
                    } catch (Throwable ex) {
                        issue = ex;
                    }

                    if (info != null || issue != null) {
                        BuilderResult builderResult = new BuilderResult(
                                info,
                                BuilderUtils.createIssue(builder, issue));
                        result.addValue(key, builderResult);
                    }
                }
            }
            return result.create();
        }

        public ActionFetchedModels executeUnsafe(EvaluatedBuild evaluatedBuild, BuildController controller) {
            AllProjectInfoBuilder builder = new AllProjectInfoBuilder(modelClasses, evaluatedBuild);

            Map<String, ActionFetchedProjectModels> fetchedModels = builder.buildProjectModels(controller);
            ActionFetchedProjectModels defaultModels = fetchedModels.remove(builder.getDefaultProjectPath());

            CustomSerializedMap buildModels = getBuildInfoResults(controller);
            return new ActionFetchedModels(buildModels, defaultModels, fetchedModels.values());
        }

        @Override
        public ActionFetchedModelsOrError execute(final BuildController controller) {
            EvaluatedBuild evaluatedBuild;
            try {
                // Note: Currently Gradle throws an exception before actually
                // executing the build action. However, if this behaviour is
                // ever changed, I expect them to be thrown here.
                evaluatedBuild = new EvaluatedBuild(controller);
            } catch (Throwable buildScriptEvaluationError) {
                return new ActionFetchedModelsOrError(null, buildScriptEvaluationError, null);
            }

            Throwable unexpected = null;
            ActionFetchedModels result = null;
            try {
                result = executeUnsafe(evaluatedBuild, controller);
            } catch (Throwable ex) {
                unexpected = ex;
            }

            return new ActionFetchedModelsOrError(result, null, unexpected);
        }
    }

    private static final class EvaluatedBuild {
        public final BuildController controller;
        public final GradleBuild buildModel;
        public final Collection<? extends BasicGradleProject> allProjects;

        public EvaluatedBuild(BuildController controller) {
            this.controller = controller;
            this.buildModel = controller.getBuildModel();
            this.allProjects = buildModel.getProjects();
        }
    }

    private static final class AllProjectInfoBuilder {
        private final Set<Class<?>> modelClasses;
        private final Map<String, BasicGradleProject> basicInfos;
        private final Map<String, ModelQueryOutput> customInfos;
        private final BasicGradleProject basicRootProject;
        private final String defaultProjectPath;

        private final SerializationCache serializationCache;

        public AllProjectInfoBuilder(Set<Class<?>> modelClasses, EvaluatedBuild evaluatedBuild) {
            int projectCount = evaluatedBuild.allProjects.size();
            this.modelClasses = modelClasses;
            this.basicInfos = CollectionUtils.newHashMap(projectCount);
            this.customInfos = CollectionUtils.newHashMap(projectCount);
            this.basicRootProject = evaluatedBuild.buildModel.getRootProject();
            this.serializationCache = SerializationCaches.getDefault();
            this.defaultProjectPath = addCustomInfo(defaultModelGetter(evaluatedBuild.controller));

            // TODO: If lazy project evaluation is available, review this
            //   not to force evaluation of unnecessary projects.
            for (BasicGradleProject project: evaluatedBuild.allProjects) {
                addBasicInfo(project);
            }
        }

        private String addCustomInfo(ModelGetter modelGetter) {
            assert serializationCache != null : "serializationCache is null in addCustomInfo";

            ModelQueryOutput customInfo = getModelOutput(serializationCache, modelGetter);
            String projectPath = customInfo.getBasicInfo().getProjectFullName();

            customInfos.put(projectPath, customInfo);
            return projectPath;
        }

        private void addBasicInfo(BasicGradleProject projectRef) {
            basicInfos.put(projectRef.getPath(), projectRef);
        }

        public String getDefaultProjectPath() {
            return defaultProjectPath;
        }

        // Note: We expect the result of this method to be mutable.
        public Map<String, ActionFetchedProjectModels> buildProjectModels(BuildController controller) {
            for (Map.Entry<String, BasicGradleProject> entry: basicInfos.entrySet()) {
                String projectPath = entry.getKey();

                if (!customInfos.containsKey(projectPath)) {
                    String addedProjectPath
                            = addCustomInfo(projectModelGetter(controller, entry.getValue()));

                    if (!projectPath.equals(addedProjectPath)) {
                        throw new IllegalStateException("The path fetched from"
                                + " the build script is different than provided"
                                + " by BasicGradleProject. BasicGradleProject.path = " + projectPath
                                + ". ModelQueryOutput.projectFullName = " + addedProjectPath);
                    }
                }
            }

            Map<String, GradleProjectTree> projectTrees = CollectionUtils.newHashMap(basicInfos.size());
            GradleProjectTree rootTree = parseTrees(controller, basicRootProject, projectTrees);

            // This should be a NO-OP because all projects should be reachable
            // from the root project. Do it anyway.
            for (BasicGradleProject project: basicInfos.values()) {
                parseTrees(controller, project, projectTrees);
            }

            Map<String, ActionFetchedProjectModels> result = CollectionUtils.newHashMap(basicInfos.size());
            for (Map.Entry<String, BasicGradleProject> entry: basicInfos.entrySet()) {
                ActionFetchedProjectModels fetchedModels
                        = getFetchedProjectModels(controller, entry, rootTree, projectTrees);
                result.put(entry.getKey(), fetchedModels);
            }
            return result;
        }

        private ActionFetchedProjectModels getFetchedProjectModels(
                BuildController controller,
                Map.Entry<String, BasicGradleProject> entry,
                GradleProjectTree rootTree,
                Map<String, GradleProjectTree> projects) {

            String projectPath = entry.getKey();

            ModelQueryOutput modelOutput = customInfos.get(projectPath);
            if (modelOutput == null) {
                throw new IllegalStateException("Missing ModelQueryOutput for project " + projectPath);
            }

            GradleProjectTree projectTree = projects.get(projectPath);
            if (projectTree == null) {
                throw new IllegalStateException("Missing GradleProjectTree for project " + projectPath);
            }

            Map<Class<?>, Object> toolingModels;

            if (modelClasses.isEmpty()) {
                toolingModels = Collections.emptyMap();
            }
            else {
                ModelGetter modelGetter = projectModelGetter(controller, entry.getValue());

                toolingModels = new IdentityHashMap<Class<?>, Object>(2 * modelClasses.size());
                for (Class<?> modelClass: modelClasses) {
                    Object modelValue = modelGetter.findModel(modelClass);
                    if (modelValue != null) {
                        toolingModels.put(modelClass, modelValue);
                    }
                }
            }

            return new ActionFetchedProjectModels(
                    new GradleMultiProjectDef(rootTree, projectTree),
                    modelOutput.getProjectInfoResults(),
                    toolingModels,
                    modelOutput.getIssue());
        }

        private GradleProjectTree parseTrees(
                BuildController controller,
                BasicGradleProject project,
                Map<String, GradleProjectTree> trees) {

            String projectPath = project.getPath();
            GradleProjectTree cached = trees.get(projectPath);
            if (cached != null) {
                return cached;
            }

            DomainObjectSet<? extends BasicGradleProject> basicChildren = project.getChildren();
            List<GradleProjectTree> children = new ArrayList<GradleProjectTree>(basicChildren.size());
            for (BasicGradleProject child: basicChildren) {
                children.add(parseTrees(controller, child, trees));
            }

            ModelQueryOutput customInfo = customInfos.get(projectPath);
            if (customInfo == null) {
                throw new IllegalStateException("Missing ModelQueryOutput for project " + projectPath);
            }

            ModelQueryOutput.BasicInfo basicInfo = customInfo.getBasicInfo();

            GenericProjectProperties genericProperties = new GenericProjectProperties(
                    basicInfo.getProjectId(),
                    projectPath,
                    project.getProjectDirectory(),
                    basicInfo.getBuildScript(),
                    basicInfo.getBuildDir());

            GradleProjectTree result = new GradleProjectTree(
                    genericProperties,
                    basicInfo.getTasks(),
                    children);

            trees.put(projectPath, result);
            return result;
        }
    }

    private interface ModelGetter {
        public <T> T findModel(Class<T> modelClass);
    }
}
