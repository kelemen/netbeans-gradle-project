package org.netbeans.gradle.project.java.query;

import java.io.File;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.java.model.JavaProjectReference;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.util.ExcludeIncludeRules;
import org.netbeans.spi.java.classpath.PathResourceImplementation;
import org.netbeans.spi.java.classpath.support.ClassPathSupport;
import org.openide.filesystems.FileUtil;

// TODO: Optimize this class by merging equivalent class path lists into a single one (or maybe concat lists?)
public final class ProjectClassPathResourceBuilder {
    private static final Logger LOGGER = Logger.getLogger(ProjectClassPathResourceBuilder.class.getName());

    private final NbJavaModel projectModel;
    private final ProjectPlatform currentPlatform;

    private Set<File> missing;
    private Map<ClassPathKey, List<PathResourceImplementation>> classpathResources;

    public ProjectClassPathResourceBuilder(NbJavaModel projectModel, ProjectPlatform currentPlatform) {
        ExceptionHelper.checkNotNullArgument(projectModel, "projectModel");
        ExceptionHelper.checkNotNullArgument(currentPlatform, "currentPlatform");

        this.projectModel = projectModel;
        this.currentPlatform = currentPlatform;
        this.classpathResources = null;
        this.missing = null;
    }

    public void build() {
        classpathResources = new HashMap<>();
        missing = new HashSet<>();

        NbJavaModule mainModule = projectModel.getMainModule();
        for (JavaSourceSet sourceSet: mainModule.getSources()) {
            loadPathResources(sourceSet, missing);
        }

        loadBootClassPath();
        loadAllRuntimeClassPath(mainModule);
        loadAllBuildOutputClassPath(projectModel);

        loadCompileForGlobalClassPath(projectModel);
        loadRuntimeForGlobalClassPath(projectModel);
    }

    private static List<PathResourceImplementation> getBuildOutputDirsAsPathResources(JavaSourceSet sourceSet) {
        JavaOutputDirs outputDirs = sourceSet.getOutputDirs();
        PathResourceImplementation classesDir = toPathResource(outputDirs.getClassesDir());
        PathResourceImplementation resourcesDir = toPathResource(outputDirs.getResourcesDir());

        List<PathResourceImplementation> result = new ArrayList<>(2);
        if (classesDir != null) result.add(classesDir);
        if (resourcesDir != null) result.add(resourcesDir);
        return result;
    }

    private void loadCompilePathResources(JavaSourceSet sourceSet, Set<File> invalid) {
        Set<File> compileCP = sourceSet.getClasspaths().getCompileClasspaths();
        setClassPathResources(
                new SourceSetClassPathType(sourceSet.getName(), ClassPathType.COMPILE),
                getPathResources(compileCP, invalid));
    }

    private void loadRuntimePathResources(JavaSourceSet sourceSet, Set<File> invalid) {
        Set<File> runtimeCP = sourceSet.getClasspaths().getRuntimeClasspaths();
        setClassPathResources(
                new SourceSetClassPathType(sourceSet.getName(), ClassPathType.RUNTIME),
                getPathResources(runtimeCP, invalid),
                getBuildOutputDirsAsPathResources(sourceSet));
    }

    private void loadSourcePathResources(JavaSourceSet sourceSet, Set<File> invalid) {
        List<PathResourceImplementation> sourcePaths = new LinkedList<>();
        for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
            Set<File> sourceRoots = sourceGroup.getSourceRoots();
            ExcludeIncludeRules includeRules = ExcludeIncludeRules.create(sourceGroup);

            sourcePaths.addAll(getPathResources(sourceRoots, invalid, includeRules));
        }

        setClassPathResources(
                new SourceSetClassPathType(sourceSet.getName(), ClassPathType.SOURCES),
                sourcePaths);
    }

    private void loadPathResources(JavaSourceSet sourceSet, Set<File> invalid) {
        loadCompilePathResources(sourceSet, invalid);
        loadRuntimePathResources(sourceSet, invalid);
        loadSourcePathResources(sourceSet, invalid);
    }

    private void loadBootClassPath() {
        List<PathResourceImplementation> platformResources = new LinkedList<>();
        for (URL url: currentPlatform.getBootLibraries()) {
            platformResources.add(ClassPathSupport.createResource(url));
        }

        setClassPathResources(SpecialClassPath.BOOT, platformResources);
    }

    private void loadAllRuntimeClassPath(NbJavaModule mainModule) {
        Set<File> classPaths = new HashSet<>();

        for (JavaSourceSet sourceSet: mainModule.getSources()) {
            classPaths.add(sourceSet.getOutputDirs().getClassesDir());
            classPaths.addAll(sourceSet.getClasspaths().getRuntimeClasspaths());
        }

        setClassPathResources(
                SpecialClassPath.ALL_RUNTIME,
                getPathResources(classPaths, new HashSet<File>()));
    }

    private void loadRuntimeForGlobalClassPath(NbJavaModel projectModel) {
        Set<File> classPaths = new HashSet<>();

        for (JavaSourceSet sourceSet: projectModel.getMainModule().getSources()) {
            classPaths.addAll(sourceSet.getClasspaths().getRuntimeClasspaths());
        }

        removeOtherBuildOutputDirs(projectModel, classPaths);

        setClassPathResources(
                SpecialClassPath.RUNTIME_FOR_GLOBAL,
                getPathResources(classPaths, new HashSet<File>()));
    }

    private void loadCompileForGlobalClassPath(NbJavaModel projectModel) {
        Set<File> classPaths = new HashSet<>();

        for (JavaSourceSet sourceSet: projectModel.getMainModule().getSources()) {
            classPaths.addAll(sourceSet.getClasspaths().getCompileClasspaths());
        }

        removeOtherBuildOutputDirs(projectModel, classPaths);

        setClassPathResources(
                SpecialClassPath.COMPILE_FOR_GLOBAL,
                getPathResources(classPaths, new HashSet<File>()));
    }

    private static void removeOtherBuildOutputDirs(NbJavaModel projectModel, Set<File> classPaths) {
        for (JavaProjectReference dependency: projectModel.getAllDependencies()) {
            NbJavaModule module = dependency.tryGetModule();
            if (module != null) {
                for (JavaSourceSet sourceSet: module.getSources()) {
                    classPaths.remove(sourceSet.getOutputDirs().getClassesDir());
                }
            }
        }
    }

    private void loadAllBuildOutputClassPath(NbJavaModel projectModel) {
        Set<File> classPaths = new HashSet<>();

        for (JavaSourceSet sourceSet: projectModel.getMainModule().getSources()) {
            classPaths.add(sourceSet.getOutputDirs().getClassesDir());
        }

        for (JavaProjectReference dependency: projectModel.getAllDependencies()) {
            dependency.ensureProjectLoaded();

            NbJavaModule module = dependency.tryGetModule();
            if (module != null) {
                for (JavaSourceSet sourceSet: module.getSources()) {
                    classPaths.add(sourceSet.getOutputDirs().getClassesDir());
                }
            }
        }

        setClassPathResources(
                SpecialClassPath.ALL_BUILD_OUTPUT,
                getPathResources(classPaths, new HashSet<File>()));
    }

    private void setClassPathResources(
            ClassPathKey classPathKey,
            List<PathResourceImplementation> paths) {
        classpathResources.put(classPathKey, Collections.unmodifiableList(paths));
    }

    private void setClassPathResources(
            ClassPathKey classPathKey,
            List<PathResourceImplementation> paths1,
            List<PathResourceImplementation> paths2) {
        List<PathResourceImplementation> paths = new ArrayList<>(paths1.size() + paths2.size());
        paths.addAll(paths1);
        paths.addAll(paths2);
        setClassPathResources(classPathKey, paths);
    }


    private static PathResourceImplementation toPathResource(File file) {
        URL url = FileUtil.urlForArchiveOrDir(file);
        return url != null ? ClassPathSupport.createResource(url) : null;
    }

    private static PathResourceImplementation toPathResource(File file, ExcludeIncludeRules includeRules) {
        return ExcludeAwarePathResource.tryCreate(file, includeRules);
    }

    private static List<PathResourceImplementation> getPathResources(
            Collection<File> files,
            Set<File> invalid) {
        return getPathResources(files, invalid, ExcludeIncludeRules.ALLOW_ALL);
    }

    public static List<PathResourceImplementation> getPathResources(
            Collection<File> files,
            Set<File> invalid,
            ExcludeIncludeRules includeRules) {
        List<PathResourceImplementation> result = new ArrayList<>(files.size());
        for (File file: new LinkedHashSet<>(files)) {
            PathResourceImplementation pathResource = includeRules.isAllowAll()
                    ? toPathResource(file)
                    : toPathResource(file, includeRules);
            // Ignore invalid classpath entries
            if (pathResource != null) {
                result.add(pathResource);
            }
            else {
                invalid.add(file);
                LOGGER.log(Level.WARNING, "Class path entry is invalid: {0}", file);
            }
        }
        return result;
    }

    private static void throwUnbuilt() {
        throw new IllegalStateException("Call build() first before calling this method.");
    }

    public Set<File> getMissing() {
        if (missing == null) {
            throwUnbuilt();
        }
        return Collections.unmodifiableSet(missing);
    }

    public Map<ClassPathKey, List<PathResourceImplementation>> getClasspathResources() {
        if (classpathResources == null) {
            throwUnbuilt();
        }
        return Collections.unmodifiableMap(classpathResources);
    }

    public enum ClassPathType {
        SOURCES,
        COMPILE,
        RUNTIME;
    }

    // Just a marker for type safety
    public static interface ClassPathKey {
    }

    public enum SpecialClassPath implements ClassPathKey {
        BOOT,
        ALL_RUNTIME,
        ALL_BUILD_OUTPUT,
        COMPILE_FOR_GLOBAL,
        RUNTIME_FOR_GLOBAL,
    }

    public static final class SourceSetClassPathType implements ClassPathKey {
        private final String sourceSetName;
        private final ClassPathType classPathType;

        public SourceSetClassPathType(String sourceSetName, ClassPathType classPathType) {
            ExceptionHelper.checkNotNullArgument(sourceSetName, "sourceSetName");
            ExceptionHelper.checkNotNullArgument(classPathType, "classPathType");

            this.sourceSetName = sourceSetName;
            this.classPathType = classPathType;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = 97 * hash + sourceSetName.hashCode();
            hash = 97 * hash + classPathType.hashCode();
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final SourceSetClassPathType other = (SourceSetClassPathType)obj;

            return this.sourceSetName.equals(other.sourceSetName)
                    && this.classPathType == other.classPathType;
        }
    }
}
