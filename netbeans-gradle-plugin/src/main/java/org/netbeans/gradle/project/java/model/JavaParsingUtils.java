package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JarOutput;
import org.netbeans.gradle.model.java.JarOutputsModel;
import org.netbeans.gradle.model.java.JavaClassPaths;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaSourcesModel;
import org.netbeans.gradle.model.java.WarFoldersModel;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.entry.ModelLoadResult;
import org.netbeans.gradle.project.model.GradleModelLoader;
import org.netbeans.gradle.project.others.OtherPlugins;
import org.openide.util.Lookup;

public final class JavaParsingUtils {
    private static final Logger LOGGER = Logger.getLogger(JavaParsingUtils.class.getName());

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

    private static Map<File, File> getJarsToBuildDirs(ModelLoadResult buildInfo) {
        Map<File, Lookup> allProjects = buildInfo.getEvaluatedProjectsModel();

        Map<File, File> result = CollectionUtils.newHashMap(allProjects.size());
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
        result.setCompileClassPathProblem(sourceSet.getCompileClassPathProblem());
        result.setRuntimeClassPathProblem(sourceSet.getRuntimeClassPathProblem());

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

    private static NbGradleProject getProject(File projectDir) {
        return GradleModelLoader.tryFindGradleProject(projectDir);
    }

    private static List<NbListedDir> getListedDirs(ModelLoadResult retrievedModels, Lookup projectInfo) {
        List<NbListedDir> listedDirs = new LinkedList<NbListedDir>();

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
        Map<File, File> jarsToBuildDirs = getJarsToBuildDirs(retrievedModels);

        Map<File, Lookup> allProjects = retrievedModels.getEvaluatedProjectsModel();

        List<NbJavaModule> result = new ArrayList<NbJavaModule>(allProjects.size());
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
            }

            Collection<JavaSourceSet> sourceSets = adjustedSources(sourcesModel, jarsToBuildDirs);
            List<NbListedDir> listedDirs = getListedDirs(retrievedModels, projectInfo);

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

    private JavaParsingUtils() {
        throw new AssertionError();
    }
}
