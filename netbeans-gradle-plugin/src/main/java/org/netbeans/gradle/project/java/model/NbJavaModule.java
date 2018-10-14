package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Supplier;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.utils.LazyValues;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.java.test.TestTaskName;

public final class NbJavaModule implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GenericProjectProperties properties;
    private final JavaCompatibilityModel compatibilityModel;
    private final List<JavaSourceSet> sources;
    private final List<NbListedDir> listedDirs;
    private final List<NbJarOutput> jarOutputs;
    private final JavaTestModel testTasks;
    private final NbCodeCoverage codeCoverage;

    private final Supplier<JavaSourceSet> mainSourceSetRef;
    private final Supplier<JavaSourceSet> testSourceSetRef;
    private final Supplier<List<JavaSourceSet>> testSourceSetsRef;
    private final Supplier<List<JavaSourceSet>> nonTestSourceSetsRef;
    private final Supplier<List<NamedSourceRoot>> namedSourceRootsRef;
    private final Supplier<Map<String, JavaSourceSet>> nameToSourceSetRef;
    private final Supplier<Map<String, JavaTestTask>> testNameToModelRef;
    private final Supplier<Set<File>> allBuildOutputDirsRefs;
    private final Supplier<Set<File>> allBuildOutputRefs;
    private final Supplier<Map<File, List<JavaSourceSet>>> outputsToSourceSets;
    private final Supplier<Map<File, List<JavaSourceSet>>> buildOutputToSourceSets;
    private final Supplier<Map<File, List<JavaSourceSet>>> jarOutputsToSourceSets;
    private final Supplier<Map<File, File>> classesDirToJars;

    public NbJavaModule(
            GenericProjectProperties properties,
            JavaCompatibilityModel compatibilityModel,
            Collection<JavaSourceSet> sources,
            List<NbListedDir> listedDirs,
            List<NbJarOutput> jarOutputs,
            JavaTestModel testTasks,
            NbCodeCoverage codeCoverage) {

        this.properties = Objects.requireNonNull(properties, "properties");
        this.compatibilityModel = Objects.requireNonNull(compatibilityModel, "compatibilityModel");
        this.sources = CollectionUtils.copyNullSafeList(sources);
        this.listedDirs = CollectionUtils.copyNullSafeList(listedDirs);
        this.jarOutputs = CollectionUtils.copyNullSafeList(jarOutputs);
        this.testTasks = Objects.requireNonNull(testTasks, "testTasks");
        this.codeCoverage = Objects.requireNonNull(codeCoverage, "codeCoverage");

        this.mainSourceSetRef = LazyValues.lazyValue(() -> findByNameOrEmpty(JavaSourceSet.NAME_MAIN));
        this.testSourceSetRef = LazyValues.lazyValue(() -> findByNameOrEmpty(JavaSourceSet.NAME_TEST));
        this.testSourceSetsRef = LazyValues.lazyValue(this::findTestSourceSets);
        this.nonTestSourceSetsRef = LazyValues.lazyValue(this::findNonTestSourceSets);
        this.namedSourceRootsRef = LazyValues.lazyValue(() -> {
            return Collections.unmodifiableList(NamedSourceRoot.getAllSourceRoots(this));
        });
        this.nameToSourceSetRef = LazyValues.lazyValue(this::createNameToSourceSet);
        this.testNameToModelRef = LazyValues.lazyValue(this::createTestNameToModel);
        this.allBuildOutputRefs = LazyValues.lazyValue(this::createAllBuildOutputs);
        this.allBuildOutputDirsRefs = LazyValues.lazyValue(this::createAllBuildOutputDirs);
        this.outputsToSourceSets = LazyValues.lazyValue(this::createOutputsToSourceSets);
        this.buildOutputToSourceSets = LazyValues.lazyValue(this::createBuildOutputsToSourceSets);
        this.jarOutputsToSourceSets = LazyValues.lazyValue(this::createJarOutputsToSourceSets);
        this.classesDirToJars = LazyValues.lazyValue(this::createClassesDirToJar);
    }

    public GenericProjectProperties getProperties() {
        return properties;
    }

    public NbCodeCoverage getCodeCoverage() {
        return codeCoverage;
    }

    public JavaTestModel getTestTasks() {
        return testTasks;
    }

    public JavaCompatibilityModel getCompatibilityModel() {
        return compatibilityModel;
    }

    public File getModuleDir() {
        return properties.getProjectDir();
    }

    public String getShortName() {
        return properties.getProjectName();
    }

    public String getUniqueName() {
        return properties.getProjectFullName();
    }

    public List<JavaSourceSet> getSources() {
        return sources;
    }

    public List<NbListedDir> getListedDirs() {
        return listedDirs;
    }

    public List<NbJarOutput> getJarOutputs() {
        return jarOutputs;
    }

    private Map<File, List<JavaSourceSet>> createBuildOutputsToSourceSets() {
        Map<File, List<JavaSourceSet>> result = new HashMap<>();

        for (JavaSourceSet sourceSet: sources) {
            JavaOutputDirs outputDirs = sourceSet.getOutputDirs();
            for (File classesDir: outputDirs.getClassesDirs()) {
                result.put(classesDir, Collections.singletonList(sourceSet));
            }
        }

        return result;
    }

    private Map<File, List<JavaSourceSet>> getBuildOutputsToProjectDeps() {
        return buildOutputToSourceSets.get();
    }

    private Map<File, List<JavaSourceSet>> createOutputsToSourceSets() {
        Map<File, List<JavaSourceSet>> result = new HashMap<>();
        result.putAll(getBuildOutputsToProjectDeps());
        result.putAll(getJarOutputsToProjectDeps());
        return result;
    }

    private Map<File, List<JavaSourceSet>> getOutputsToProjectDeps() {
        return outputsToSourceSets.get();
    }

    public List<JavaSourceSet> getSourceSetsForOutput(File outputPath) {
        List<JavaSourceSet> result = getOutputsToProjectDeps().get(outputPath);
        return result != null ? result : Collections.<JavaSourceSet>emptyList();
    }

    private Map<File, File> createClassesDirToJar() {
        Map<File, File> result = new HashMap<>();
        jarOutputs.forEach(jar -> {
            jar.getClassDirs().forEach(classesDir -> result.put(classesDir, jar.getJar()));
        });
        return result;
    }

    private Map<File, List<JavaSourceSet>> createJarOutputsToSourceSets() {
        Map<File, List<JavaSourceSet>> buildOutputsToProjectDeps = getBuildOutputsToProjectDeps();

        Map<File, List<JavaSourceSet>> result = new HashMap<>();

        for (NbJarOutput jar: jarOutputs) {
            Set<File> classDirs = jar.getClassDirs();
            Set<JavaSourceSet> sourceSets = new LinkedHashSet<>(2 * classDirs.size());
            for (File classDir: classDirs) {
                List<JavaSourceSet> classDirSourceSets = buildOutputsToProjectDeps.get(classDir);
                if (classDirSourceSets != null) {
                    sourceSets.addAll(classDirSourceSets);
                }
            }
            result.put(jar.getJar(), CollectionsEx.readOnlyCopy(sourceSets));
        }
        return result;
    }

    private Map<File, List<JavaSourceSet>> getJarOutputsToProjectDeps() {
        return jarOutputsToSourceSets.get();
    }

    public File tryGetJarForOutput(JavaOutputDirs outputDirs) {
        for (File classesDir: outputDirs.getClassesDirs()) {
            File jar = tryGetJarForClassesDir(classesDir);
            if (jar != null) {
                return jar;
            }
        }
        return null;
    }

    public File tryGetJarForClassesDir(File classesDir) {
        Objects.requireNonNull(classesDir, "classesDir");
        return classesDirToJars.get().get(classesDir);
    }

    public List<JavaSourceSet> getSourceSetsForJarOutput(File jarPath) {
        List<JavaSourceSet> result = getJarOutputsToProjectDeps().get(jarPath);
        return result != null ? result : Collections.<JavaSourceSet>emptyList();
    }

    private JavaSourceSet emptySourceSet(String name) {
        File classesDir = new File(properties.getProjectDir(), "nb-virtual-classes");
        File resourcesDir = new File(properties.getProjectDir(), "nb-virtual-resources");
        JavaOutputDirs output = new JavaOutputDirs(
                Collections.singleton(classesDir),
                resourcesDir,
                Collections.<File>emptyList());
        JavaSourceSet.Builder result = new JavaSourceSet.Builder(name, output);
        return result.create();
    }

    private JavaSourceSet findByNameOrEmpty(String name) {
        for (JavaSourceSet sourceSet: sources) {
            if (sourceSet.getName().equals(name)) {
                return sourceSet;
            }
        }

        return emptySourceSet(name);
    }

    public JavaSourceSet getMainSourceSet() {
        return mainSourceSetRef.get();
    }

    public JavaSourceSet getTestSourceSet() {
        return testSourceSetRef.get();
    }

    private List<JavaSourceSet> findSourceSets(SourceSetFilter filter) {
        List<JavaSourceSet> result = new ArrayList<>(sources.size());
        for (JavaSourceSet sourceSet: sources) {
            if (!filter.needSourceSet(sourceSet)) {
                continue;
            }

            if (filter.isPriority(sourceSet)) {
                result.add(0, sourceSet);
            }
            else {
                result.add(sourceSet);
            }
        }
        return CollectionUtils.copyNullSafeList(result);
    }

    private List<JavaSourceSet> findTestSourceSets() {
        return findSourceSets(new SourceSetFilter() {
            @Override
            public boolean needSourceSet(JavaSourceSet sourceSet) {
                return JavaSourceGroupID.isTestSourceSet(sourceSet.getName());
            }

            @Override
            public boolean isPriority(JavaSourceSet sourceSet) {
                return JavaSourceSet.NAME_TEST.equals(sourceSet.getName());
            }
        });
    }

    public List<JavaSourceSet> getTestSourceSets() {
        return testSourceSetsRef.get();
    }

    private List<JavaSourceSet> findNonTestSourceSets() {
        return findSourceSets(new SourceSetFilter() {
            @Override
            public boolean needSourceSet(JavaSourceSet sourceSet) {
                return !JavaSourceGroupID.isTestSourceSet(sourceSet.getName());
            }

            @Override
            public boolean isPriority(JavaSourceSet sourceSet) {
                return JavaSourceSet.NAME_MAIN.equals(sourceSet.getName());
            }
        });
    }

    public List<JavaSourceSet> getNonTestSourceSets() {
        return nonTestSourceSetsRef.get();
    }

    public List<NamedSourceRoot> getNamedSourceRoots() {
        return namedSourceRootsRef.get();
    }

    private Map<String, JavaSourceSet> createNameToSourceSet() {
        Map<String, JavaSourceSet> result = CollectionUtils.newHashMap(sources.size());
        for (JavaSourceSet sourceSet: sources) {
            result.put(sourceSet.getName(), sourceSet);
        }
        return result;
    }

    private Map<String, JavaSourceSet> getNameToSourceSet() {
        return nameToSourceSetRef.get();
    }

    public JavaSourceSet tryGetSourceSetByName(String name) {
        Objects.requireNonNull(name, "name");
        return getNameToSourceSet().get(name);
    }

    private Map<String, JavaTestTask> createTestNameToModel() {
        Map<String, JavaTestTask> result = CollectionUtils.newHashMap(sources.size());
        for (JavaTestTask testTask: testTasks.getTestTasks()) {
            result.put(testTask.getName(), testTask);
        }
        return result;
    }

    private Map<String, JavaTestTask> getTestNameToModel() {
        return testNameToModelRef.get();
    }

    public JavaTestTask getTestModelByName(String name) {
        JavaTestTask testTask = tryGetTestModelByName(name);
        return testTask != null
                ? testTask
                : JavaTestTask.getDefaulTestModel(getModuleDir());
    }

    public JavaTestTask tryGetTestModelByName(String name) {
        Objects.requireNonNull(name, "name");
        return getTestNameToModel().get(name);
    }

    private Set<File> createAllBuildOutputDirs() {
        Set<File> result = CollectionUtils.newHashSet(sources.size());
        for (JavaSourceSet sourceSet: sources) {
            result.addAll(sourceSet.getOutputDirs().getClassesDirs());
        }
        return Collections.unmodifiableSet(result);
    }

    public Set<File> getAllBuildOutputDirs() {
        return allBuildOutputDirsRefs.get();
    }

    private Set<File> createAllBuildOutputs() {
        Set<File> result = new HashSet<>(getAllBuildOutputDirs());

        for (NbJarOutput jar: jarOutputs) {
            result.add(jar.getJar());
        }
        return Collections.unmodifiableSet(result);
    }

    public Set<File> getAllBuildOutputs() {
        return allBuildOutputRefs.get();
    }

    private static String getExpectedTestName(String sourceSetName) {
        String result;
        if (JavaSourceSet.NAME_MAIN.equals(sourceSetName)) {
            result = TestTaskName.DEFAULT_TEST_TASK_NAME;
        }
        else {
            result = sourceSetName;
        }

        return result.toLowerCase(Locale.ROOT);
    }

    private static boolean isFirstCloserInLength(String name1, String name2, String expected) {
        int diff1 = name1.length() - expected.length();
        int diff2 = name2.length() - expected.length();

        return Math.abs(diff1) < Math.abs(diff2);
    }

    public String findTestTaskForSourceSet(String sourceSetName) {
        Objects.requireNonNull(sourceSetName, "sourceSetName");

        String expectedName = getExpectedTestName(sourceSetName);

        String bestName = null;
        for (JavaTestTask task: testTasks.getTestTasks()) {
            String taskName = task.getName();
            String normTaskName = taskName.toLowerCase(Locale.ROOT);
            if (normTaskName.startsWith(expectedName)
                    || expectedName.startsWith(normTaskName)) {
                if (bestName == null || isFirstCloserInLength(taskName, bestName, expectedName)) {
                    bestName = taskName;
                }
            }
        }

        return bestName != null ? bestName : TestTaskName.DEFAULT_TEST_TASK_NAME;
    }

    public Map<String, Set<String>> sourceSetDependencyGraph() {
        Map<File, String> buildOutput = new LinkedHashMap<>();

        getSources().forEach(sourceSet -> {
            sourceSet.getOutputDirs().getClassesDirs().forEach(classesDir -> {
                buildOutput.put(classesDir, sourceSet.getName());
            });
        });

        Map<String, Set<String>> result = new LinkedHashMap<>();
        getSources().forEach(sourceSet -> {
            String sourceSetName = sourceSet.getName();

            Set<File> compileClasspaths = sourceSet.getClasspaths().getCompileClasspaths();
            Set<File> runtimeClasspaths = sourceSet.getClasspaths().getRuntimeClasspaths();

            Set<String> dependencies = new LinkedHashSet<>();
            buildOutput.forEach((classesOutputDir, dependencyName) -> {
                if (runtimeClasspaths.contains(classesOutputDir) || compileClasspaths.contains(classesOutputDir)) {
                    if (!sourceSetName.equals(dependencyName)) {
                        dependencies.add(dependencyName);
                    }
                }
            });

            result.put(sourceSetName, dependencies);
        });

        // TODO: Remove redundant edges

        return result;
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final GenericProjectProperties properties;
        private final JavaCompatibilityModel compatibilityModel;
        private final List<JavaSourceSet> sources;
        private final List<NbListedDir> listedDirs;
        private final List<NbJarOutput> jarOutputs;
        private final JavaTestModel testTasks;
        private final NbCodeCoverage codeCoverage;

        public SerializedFormat(NbJavaModule source) {
            this.properties = source.properties;
            this.compatibilityModel = source.compatibilityModel;
            this.sources = source.sources;
            this.listedDirs = source.listedDirs;
            this.jarOutputs = source.jarOutputs;
            this.testTasks = source.testTasks;
            this.codeCoverage = source.codeCoverage;
        }

        private NbCodeCoverage getCodeCoverage() {
            return codeCoverage != null ? codeCoverage : NbCodeCoverage.NO_CODE_COVERAGE;
        }

        private List<NbJarOutput> jarOutputs() {
            return jarOutputs != null ? jarOutputs : Collections.<NbJarOutput>emptyList();
        }


        private Object readResolve() throws ObjectStreamException {
            return new NbJavaModule(
                    properties,
                    compatibilityModel,
                    sources,
                    listedDirs,
                    jarOutputs(),
                    testTasks,
                    getCodeCoverage());
        }
    }

    private interface SourceSetFilter {
        public boolean needSourceSet(JavaSourceSet sourceSet);
        public boolean isPriority(JavaSourceSet sourceSet);
    }
}
