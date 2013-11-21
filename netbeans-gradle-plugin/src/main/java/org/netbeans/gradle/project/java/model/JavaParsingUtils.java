package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.GradleBuildInfoQuery;
import org.netbeans.gradle.model.api.GradleProjectInfoQuery;
import org.netbeans.gradle.model.api.ModelClassPathDef;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.java.JarOutput;
import org.netbeans.gradle.model.java.JarOutputsModel;
import org.netbeans.gradle.model.java.JarOutputsModelBuilder;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaCompatibilityModelBuilder;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaSourcesModel;
import org.netbeans.gradle.model.java.JavaSourcesModelBuilder;
import org.netbeans.gradle.model.java.WarFoldersModel;
import org.netbeans.gradle.model.java.WarFoldersModelBuilder;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.model.CustomModelQuery;
import org.netbeans.gradle.project.model.GradleBuildInfo;
import org.netbeans.gradle.project.model.GradleProjectInfo;

public final class JavaParsingUtils {
    private static final Logger LOGGER = Logger.getLogger(JavaParsingUtils.class.getName());

    private static final Integer KEY_SOURCES = 0;
    private static final Integer KEY_VERSIONS = 1;
    private static final Integer KEY_JAR_OUTPUTS = 2;
    private static final Integer KEY_WAR_FOLDERS = 3;

    public static CustomModelQuery requiredModels() {
        return RequiredModels.INSTANCE;
    }

    public static boolean isJavaProject(GradleProjectInfo projectInfo) {
        return projectInfo.tryGetProjectInfoResult(KEY_SOURCES) != null
                && projectInfo.tryGetProjectInfoResult(KEY_VERSIONS) != null
                && projectInfo.tryGetProjectInfoResult(KEY_JAR_OUTPUTS) != null;
    }

    private static JavaSourceSet tryGetSourceSetForJar(JarOutput jar, JavaSourcesModel sources) {
        String taskName = jar.getTaskName().toLowerCase(Locale.ROOT);
        if ("jar".equals(taskName)) {
            for (JavaSourceSet sourceSet: sources.getSourceSets()) {
                if (JavaSourceSet.NAME_MAIN.equals(sourceSet.getName())) {
                    return sourceSet;
                }
            }
            return null;
        }
        else {
            JavaSourceSet bestMatch = null;
            for (JavaSourceSet sourceSet: sources.getSourceSets()) {
                String sourceSetName = sourceSet.getName().toLowerCase(Locale.ROOT);
                if (taskName.contains(sourceSetName)) {
                    if (bestMatch == null || bestMatch.getName().length() < sourceSetName.length()) {
                        bestMatch = sourceSet;
                    }
                }
            }
            return bestMatch;
        }
    }

    private static File tryGetBuildDirSourceForJar(JarOutput jar, JavaSourcesModel sources) {
        JavaSourceSet result = tryGetSourceSetForJar(jar, sources);
        return result != null ? result.getOutputDirs().getClassesDir() : null;
    }

    private static Map<File, File> getJarsToBuildDirs(GradleBuildInfo buildInfo) {
        Map<File, GradleProjectInfo> allProjects = buildInfo.getAllProjectInfos();

        Map<File, File> result = CollectionUtils.newHashMap(allProjects.size());
        for (GradleProjectInfo projectInfo: allProjects.values()) {
            JarOutputsModel jarsModel = RequiredModels.extractJars(projectInfo);
            if (jarsModel != null) {
                JavaSourcesModel sources = RequiredModels.extractSources(projectInfo);
                if (sources == null) {
                    String projectName = projectInfo
                            .getProjectDef()
                            .getMainProject()
                            .getGenericProperties()
                            .getProjectFullName();
                    LOGGER.log(Level.WARNING, "No sources for Java project: {0}", projectName);
                    continue;
                }

                for (JarOutput jar: jarsModel.getJars()) {
                    File buildDir = tryGetBuildDirSourceForJar(jar, sources);
                    if (buildDir != null) {
                        result.put(jar.getJar(), buildDir);
                    }
                }
            }
        }

        return result;
    }

    private static Collection<File> adjustedClassPaths(
            Collection<File> files,
            Map<File, File> dependencyMap) {

        List<File> result = new ArrayList<File>(files.size());
        for (File file: files) {
            File adjusted = dependencyMap.get(file);
            result.add(adjusted != null ? adjusted : file);
        }
        return result;
    }

    private static JavaSourceSet adjustedSources(
            JavaSourceSet sourceSet,
            Map<File, File> dependencyMap) {

        JavaClassPaths origClassPaths = sourceSet.getClasspaths();
        Collection<File> compile = adjustedClassPaths(origClassPaths.getCompileClasspaths(), dependencyMap);
        Collection<File> runtime = adjustedClassPaths(origClassPaths.getRuntimeClasspaths(), dependencyMap);

        runtime.remove(sourceSet.getOutputDirs().getClassesDir());
        runtime.remove(sourceSet.getOutputDirs().getResourcesDir());

        JavaClassPaths classPaths = new JavaClassPaths(compile, runtime);

        JavaSourceSet.Builder result = new JavaSourceSet.Builder(sourceSet.getName(), sourceSet.getOutputDirs());
        result.setClasspaths(classPaths);

        for (JavaSourceGroup group: sourceSet.getSourceGroups()) {
            result.addSourceGroup(group);
        }

        return result.create();
    }

    private static Collection<JavaSourceSet> adjustedSources(
            JavaSourcesModel sourcesModel,
            Map<File, File> dependencyMap) {

        List<JavaSourceSet> result = new LinkedList<JavaSourceSet>();
        for (JavaSourceSet sourceSet: sourcesModel.getSourceSets()) {
            result.add(adjustedSources(sourceSet, dependencyMap));
        }
        return result;
    }

    private static List<NbListedDir> getListedDirs(GradleProjectInfo projectInfo) {
        List<NbListedDir> listedDirs = new LinkedList<NbListedDir>();

        WarFoldersModel warFolders = RequiredModels.extractWarFolders(projectInfo);
        if (warFolders != null) {
            listedDirs.add(new NbListedDir(NbStrings.getWebPages(), warFolders.getWebAppDir()));
        }

        return listedDirs;
    }

    public static Collection<NbJavaModule> parseModules(GradleBuildInfo buildInfo) {
        Map<File, File> jarsToBuildDirs = getJarsToBuildDirs(buildInfo);

        Map<File, GradleProjectInfo> allProjects = buildInfo.getAllProjectInfos();

        List<NbJavaModule> result = new ArrayList<NbJavaModule>(allProjects.size());
        for (GradleProjectInfo projectInfo: allProjects.values()) {
            JavaCompatibilityModel versions = RequiredModels.extractVersions(projectInfo);
            JavaSourcesModel sourcesModel = RequiredModels.extractSources(projectInfo);
            if (versions == null || sourcesModel == null) {
                continue;
            }

            GenericProjectProperties properties = projectInfo
                    .getProjectDef()
                    .getMainProject()
                    .getGenericProperties();

            Collection<JavaSourceSet> sourceSets = adjustedSources(sourcesModel, jarsToBuildDirs);
            List<NbListedDir> listedDirs = getListedDirs(projectInfo);

            NbJavaModule module = new NbJavaModule(properties, versions, sourceSets, listedDirs);
            result.add(module);
        }

        return result;
    }

    public static Map<File, JavaProjectDependency> asDependencies(Collection<NbJavaModule> modules) {
        Map<File, JavaProjectDependency> result = new HashMap<File, JavaProjectDependency>(3 * modules.size());
        for (NbJavaModule module: modules) {
            JavaProjectReference projectRef = new JavaProjectReference(module.getModuleDir(), module);

            for (JavaSourceSet sourceSet: module.getSources()) {
                JavaProjectDependency depedency = new JavaProjectDependency(sourceSet.getName(), projectRef);
                result.put(sourceSet.getOutputDirs().getClassesDir(), depedency);
            }
        }
        return result;
    }

    private static <T> GradleProjectInfoQuery<T> createQuery(final ProjectInfoBuilder<T> builder) {
        return new SingleBuilderQuery<T>(builder);
    }

    private static class SingleBuilderQuery<T> implements GradleProjectInfoQuery<T> {
        private final ProjectInfoBuilder<T> builder;

        public SingleBuilderQuery(ProjectInfoBuilder<T> builder) {
            this.builder = builder;
        }

        @Override
        public ProjectInfoBuilder<T> getInfoBuilder() {
            return builder;
        }

        @Override
        public ModelClassPathDef getInfoClassPath() {
            return ModelClassPathDef.EMPTY;
        }
    }

    private enum RequiredModels implements CustomModelQuery {
        INSTANCE;

        private final Map<Object, GradleProjectInfoQuery<?>> projectInfoQueries;

        private RequiredModels() {
            this.projectInfoQueries = createProjectInfoQueries();
        }

        private static Map<Object, GradleProjectInfoQuery<?>> createProjectInfoQueries() {
            Map<Object, GradleProjectInfoQuery<?>> result = new HashMap<Object, GradleProjectInfoQuery<?>>();

            result.put(KEY_SOURCES, createQuery(JavaSourcesModelBuilder.COMPLETE));
            result.put(KEY_VERSIONS, createQuery(JavaCompatibilityModelBuilder.INSTANCE));
            result.put(KEY_JAR_OUTPUTS, createQuery(JarOutputsModelBuilder.INSTANCE));
            result.put(KEY_WAR_FOLDERS, createQuery(WarFoldersModelBuilder.INSTANCE));

            return Collections.unmodifiableMap(result);
        }

        private static Object fromProjectInfo(GradleProjectInfo projectInfo, Integer key) {
            List<?> result = projectInfo.tryGetProjectInfoResult(key);
            return CollectionUtils.getSingleElement(result);
        }

        public static JavaSourcesModel extractSources(GradleProjectInfo projectInfo) {
            return (JavaSourcesModel)fromProjectInfo(projectInfo, KEY_SOURCES);
        }

        public static JavaCompatibilityModel extractVersions(GradleProjectInfo projectInfo) {
            return (JavaCompatibilityModel)fromProjectInfo(projectInfo, KEY_VERSIONS);
        }

        public static JarOutputsModel extractJars(GradleProjectInfo projectInfo) {
            return (JarOutputsModel)fromProjectInfo(projectInfo, KEY_JAR_OUTPUTS);
        }

        public static WarFoldersModel extractWarFolders(GradleProjectInfo projectInfo) {
            return (WarFoldersModel)fromProjectInfo(projectInfo, KEY_WAR_FOLDERS);
        }

        @Override
        public Collection<Class<?>> getProjectModels() {
            return Collections.emptyList();
        }

        @Override
        public Map<Object, GradleBuildInfoQuery<?>> getBuildInfoQueries() {
            return Collections.emptyMap();
        }

        @Override
        public Map<Object, GradleProjectInfoQuery<?>> getProjectInfoQueries() {
            return projectInfoQueries;
        }
    }

    private JavaParsingUtils() {
        throw new AssertionError();
    }
}
