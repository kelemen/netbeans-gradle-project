package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JacocoModel;
import org.netbeans.gradle.model.java.JarOutput;
import org.netbeans.gradle.model.java.JarOutputsModel;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaSourcesModel;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.java.WarFoldersModel;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.entry.ModelLoadResult;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.model.NbGenericModelInfo;
import org.netbeans.gradle.project.others.OtherPlugins;
import org.netbeans.gradle.project.properties.standard.SourceLevelProperty;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.GradleVersions;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;

public final class JavaParsingUtils {
    private static final Logger LOGGER = Logger.getLogger(JavaParsingUtils.class.getName());

    private static Collection<JavaSourceSet> getSourceSetsForJar(JarOutput jar, JavaSourcesModel sources) {
        Set<String> sourceSetNames = jar.tryGetSourceSetNames();
        if (sourceSetNames != null) {
            int numberOfSourceSets = sourceSetNames.size();
            Set<JavaSourceSet> result = CollectionUtils.newHashSet(numberOfSourceSets);
            for (JavaSourceSet sourceSet: sources.getSourceSets()) {
                if (sourceSetNames.contains(sourceSet.getName())) {
                    result.add(sourceSet);
                    // There is no reason to look for other source sets,
                    // we've already found all of them.
                    if (numberOfSourceSets == result.size()) {
                        break;
                    }
                }
            }
            return result;
        }

        JavaSourceSet heuristicSourceSet = tryGetSourceSetForJarWithHeuristic(jar, sources);
        return heuristicSourceSet != null
                ? Collections.singleton(heuristicSourceSet)
                : Collections.<JavaSourceSet>emptySet();
    }

    private static JavaSourceSet tryGetSourceSetForJarWithHeuristic(JarOutput jar, JavaSourcesModel sources) {
        String taskName = jar.getTaskName().toLowerCase(Locale.ROOT);
        if (NbJarOutput.DEFAULT_JAR_TASK_NAME.equals(taskName)) {
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

    private static Set<File> tryGetBuildDirSourceForJar(JarOutput jar, JavaSourcesModel sources) {
        Collection<JavaSourceSet> sourceSets = getSourceSetsForJar(jar, sources);
        if (sourceSets.isEmpty()) {
            return null;
        }

        Set<File> result = CollectionUtils.newHashSet(sourceSets.size());
        for (JavaSourceSet sourceSet: sourceSets) {
            result.addAll(sourceSet.getOutputDirs().getClassesDirs());
        }
        return result;
    }

    private static Map<File, Set<File>> getJarsToBuildDirs(ModelLoadResult buildInfo) {
        Map<File, Lookup> allProjects = buildInfo.getEvaluatedProjectsModel();

        Map<File, Set<File>> result = CollectionUtils.newHashMap(allProjects.size());
        for (Lookup projectInfo: allProjects.values()) {
            JarOutputsModel jarsModel = projectInfo.lookup(JarOutputsModel.class);
            if (jarsModel != null) {
                JavaSourcesModel sources = projectInfo.lookup(JavaSourcesModel.class);
                if (sources == null) {
                    GenericProjectProperties properties = projectInfo.lookup(GenericProjectProperties.class);
                    String projectName = properties != null
                            ? properties.getProjectFullName()
                            : "???";
                    LOGGER.log(Level.WARNING, "No sources for Java project: {0}", projectName);
                    continue;
                }

                for (JarOutput jar: jarsModel.getJars()) {
                    Set<File> buildDirs = tryGetBuildDirSourceForJar(jar, sources);
                    if (buildDirs != null) {
                        result.put(jar.getJar(), buildDirs);
                    }
                }
            }
        }

        return result;
    }

    private static Collection<File> adjustedClassPaths(
            Collection<File> files,
            Map<File, ? extends Collection<File>> dependencyMap) {

        List<File> result = new ArrayList<>(files.size());
        for (File file: files) {
            Collection<File> adjusted = dependencyMap.get(file);
            if (adjusted != null) {
                result.addAll(adjusted);
            }
            else {
                result.add(file);
            }
        }
        return result;
    }

    private static JavaSourceSet adjustedSources(
            JavaSourceSet sourceSet,
            Map<File, ? extends Collection<File>> dependencyMap) {

        JavaClassPaths origClassPaths = sourceSet.getClasspaths();
        Collection<File> compile = adjustedClassPaths(origClassPaths.getCompileClasspaths(), dependencyMap);
        Collection<File> runtime = adjustedClassPaths(origClassPaths.getRuntimeClasspaths(), dependencyMap);

        runtime.removeAll(sourceSet.getOutputDirs().getClassesDirs());
        runtime.remove(sourceSet.getOutputDirs().getResourcesDir());

        JavaClassPaths classPaths = new JavaClassPaths(compile, runtime);

        JavaSourceSet.Builder result = new JavaSourceSet.Builder(sourceSet.getName(), sourceSet.getOutputDirs());
        result.setClasspaths(classPaths);
        result.setCompileClassPathProblem(sourceSet.getCompileClassPathProblem());
        result.setRuntimeClassPathProblem(sourceSet.getRuntimeClassPathProblem());

        for (JavaSourceGroup group: sourceSet.getSourceGroups()) {
            result.addSourceGroup(group);
        }

        return result.create();
    }

    private static Collection<JavaSourceSet> adjustedSources(
            JavaSourcesModel sourcesModel,
            Map<File, ? extends Collection<File>> dependencyMap) {

        List<JavaSourceSet> result = new ArrayList<>();
        for (JavaSourceSet sourceSet: sourcesModel.getSourceSets()) {
            result.add(adjustedSources(sourceSet, dependencyMap));
        }
        return result;
    }

    private static NbGradleProject getProject(File projectDir) {
        return NbGradleProjectFactory.tryLoadSafeGradleProject(projectDir);
    }

    private static List<NbListedDir> getListedDirs(ModelLoadResult retrievedModels, Lookup projectInfo) {
        List<NbListedDir> listedDirs = new ArrayList<>();

        NbGradleProject project = getProject(retrievedModels.getMainProjectDir());
        if (project == null || !OtherPlugins.hasJavaEEExtension(project)) {
            WarFoldersModel warFolders = projectInfo.lookup(WarFoldersModel.class);
            if (warFolders != null) {
                listedDirs.add(new NbListedDir(NbStrings.getWebPages(), warFolders.getWebAppDir()));
            }
        }

        return listedDirs;
    }

    public static Collection<NbJavaModule> parseModules(ModelLoadResult retrievedModels) {
        Map<File, Set<File>> jarsToBuildDirs = getJarsToBuildDirs(retrievedModels);

        Map<File, Lookup> allProjects = retrievedModels.getEvaluatedProjectsModel();

        List<NbJavaModule> result = new ArrayList<>(allProjects.size());
        for (Lookup projectInfo: allProjects.values()) {
            JavaCompatibilityModel versions = projectInfo.lookup(JavaCompatibilityModel.class);
            JavaSourcesModel sourcesModel = projectInfo.lookup(JavaSourcesModel.class);
            if (versions == null || sourcesModel == null) {
                continue;
            }

            if (sourcesModel.getSourceSets().isEmpty()) {
                LOGGER.log(Level.INFO,
                        "Disabling the Java extension because there are no sources: {0}",
                        retrievedModels.getMainProjectDir());
                continue;
            }

            GenericProjectProperties properties = projectInfo.lookup(GenericProjectProperties.class);
            if (properties == null) {
                LOGGER.log(Level.WARNING,
                        "Missing GenericProjectProperties for project {0}",
                        retrievedModels.getMainProjectDir());
                continue;
            }

            Collection<JavaSourceSet> sourceSets = adjustedSources(sourcesModel, jarsToBuildDirs);
            List<NbListedDir> listedDirs = getListedDirs(retrievedModels, projectInfo);

            JavaTestModel testModel = projectInfo.lookup(JavaTestModel.class);
            if (testModel == null) {
                LOGGER.log(Level.WARNING,
                        "Missing JavaTestModel for project {0}",
                        retrievedModels.getMainProjectDir());
                testModel = JavaTestModel.getDefaulTestModel(retrievedModels.getMainProjectDir());
            }

            NbJavaModule module = new NbJavaModule(
                    properties,
                    versions,
                    sourceSets,
                    listedDirs,
                    getJarOutputs(projectInfo, jarsToBuildDirs),
                    testModel,
                    getCodeCoverage(projectInfo));
            result.add(module);
        }

        return result;
    }

    private static List<NbJarOutput> getJarOutputs(Lookup projectInfo, Map<File, Set<File>> jarsToBuildDirs) {
        JarOutputsModel model = projectInfo.lookup(JarOutputsModel.class);
        if (model == null) {
            return Collections.emptyList();
        }

        Collection<JarOutput> jars = model.getJars();
        List<NbJarOutput> result = new ArrayList<>(jars.size());

        for (JarOutput output: jars) {
            Set<File> buildDirs = jarsToBuildDirs.get(output.getJar());
            if (buildDirs == null) {
                buildDirs = Collections.emptySet();
            }

            result.add(new NbJarOutput(output.getTaskName(), output.getJar(), buildDirs));
        }

        return result;
    }

    private static NbCodeCoverage getCodeCoverage(Lookup projectInfo) {
        JacocoModel jacocoModel = projectInfo.lookup(JacocoModel.class);
        return new NbCodeCoverage(jacocoModel);
    }

    public static NbJavaModel createEmptyModel(FileObject projectDir, ScriptFileProvider scriptProvider) {
        File projectDirAsFile = FileUtil.toFile(projectDir);
        if (projectDirAsFile == null) {
            throw new IllegalStateException("Project directory does not exist.");
        }
        return createEmptyModel(projectDirAsFile.toPath(), scriptProvider);
    }

    private static NbJavaModel createUnreliableModel(GradleTarget evaluationEnvironment, NbJavaModule mainModule) {
        return NbJavaModel.createModel(evaluationEnvironment, JavaModelSource.COMPATIBLE_API, mainModule);
    }

    private static NbJavaModel createEmptyModel(Path projectDir, ScriptFileProvider scriptProvider) {
        String name = NbFileUtils.getFileNameStr(projectDir);
        String level = SourceLevelProperty.getSourceLevelFromPlatform(JavaPlatform.getDefault());

        GenericProjectProperties properties = NbGenericModelInfo.createProjectProperties(name, name, projectDir, scriptProvider);
        JavaCompatibilityModel compatibilityModel = new JavaCompatibilityModel(level, level);

        NbJavaModule result = new NbJavaModule(
                properties,
                compatibilityModel,
                Collections.<JavaSourceSet>emptyList(),
                Collections.<NbListedDir>emptyList(),
                Collections.<NbJarOutput>emptyList(),
                JavaTestModel.getDefaulTestModel(properties.getProjectDir()),
                NbCodeCoverage.NO_CODE_COVERAGE
        );

        return createUnreliableModel(GradleVersions.DEFAULT_TARGET, result);
    }

    private JavaParsingUtils() {
        throw new AssertionError();
    }
}
