package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;
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
    private final JavaTestModel testTasks;
    private final NbCodeCoverage codeCoverage;

    private final AtomicReference<JavaSourceSet> mainSourceSetRef;
    private final AtomicReference<JavaSourceSet> testSourceSetRef;
    private final AtomicReference<List<JavaSourceSet>> testSourceSetsRef;
    private final AtomicReference<List<JavaSourceSet>> nonTestSourceSetsRef;
    private final AtomicReference<List<NamedSourceRoot>> namedSourceRootsRef;
    private final AtomicReference<Map<String, JavaSourceSet>> nameToSourceSetRef;
    private final AtomicReference<Map<String, JavaTestTask>> testNameToModelRef;
    private final AtomicReference<Set<File>> allBuildOutputRefs;

    public NbJavaModule(
            GenericProjectProperties properties,
            JavaCompatibilityModel compatibilityModel,
            Collection<JavaSourceSet> sources,
            List<NbListedDir> listedDirs,
            JavaTestModel testTasks,
            NbCodeCoverage codeCoverage) {

        ExceptionHelper.checkNotNullArgument(properties, "properties");
        ExceptionHelper.checkNotNullArgument(compatibilityModel, "compatibilityModel");
        ExceptionHelper.checkNotNullElements(sources, "sources");
        ExceptionHelper.checkNotNullElements(listedDirs, "listedDirs");
        ExceptionHelper.checkNotNullArgument(testTasks, "testTasks");
        ExceptionHelper.checkNotNullArgument(codeCoverage, "codeCoverage");

        this.properties = properties;
        this.compatibilityModel = compatibilityModel;
        this.sources = CollectionUtils.copyNullSafeList(sources);
        this.listedDirs = CollectionUtils.copyNullSafeList(listedDirs);
        this.testTasks = testTasks;
        this.codeCoverage = codeCoverage;

        this.mainSourceSetRef = new AtomicReference<>(null);
        this.testSourceSetRef = new AtomicReference<>(null);
        this.testSourceSetsRef = new AtomicReference<>(null);
        this.nonTestSourceSetsRef = new AtomicReference<>(null);
        this.namedSourceRootsRef = new AtomicReference<>(null);
        this.nameToSourceSetRef = new AtomicReference<>(null);
        this.testNameToModelRef = new AtomicReference<>(null);
        this.allBuildOutputRefs = new AtomicReference<>(null);
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

    private JavaSourceSet emptySourceSet(String name) {
        File classesDir = new File(properties.getProjectDir(), "nb-virtual-classes");
        File resourcesDir = new File(properties.getProjectDir(), "nb-virtual-resources");
        JavaOutputDirs output = new JavaOutputDirs(classesDir, resourcesDir, Collections.<File>emptyList());
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
        JavaSourceSet result = mainSourceSetRef.get();
        if (result == null) {
            mainSourceSetRef.set(findByNameOrEmpty(JavaSourceSet.NAME_MAIN));
            result = mainSourceSetRef.get();
        }
        return result;
    }

    public JavaSourceSet getTestSourceSet() {
        JavaSourceSet result = testSourceSetRef.get();
        if (result == null) {
            testSourceSetRef.set(findByNameOrEmpty(JavaSourceSet.NAME_TEST));
            result = testSourceSetRef.get();
        }
        return result;
    }

    private List<JavaSourceSet> findSourceSets(SourceSetFilter filter) {
        List<JavaSourceSet> result = new LinkedList<>();
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
        List<JavaSourceSet> result = testSourceSetsRef.get();
        if (result == null) {
            testSourceSetsRef.set(findTestSourceSets());
            result = testSourceSetsRef.get();
        }
        return result;
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
        List<JavaSourceSet> result = nonTestSourceSetsRef.get();
        if (result == null) {
            nonTestSourceSetsRef.set(findNonTestSourceSets());
            result = nonTestSourceSetsRef.get();
        }
        return result;
    }

    public List<NamedSourceRoot> getNamedSourceRoots() {
        List<NamedSourceRoot> result = namedSourceRootsRef.get();
        if (result == null) {
            namedSourceRootsRef.set(Collections.unmodifiableList(NamedSourceRoot.getAllSourceRoots(this)));
            result = namedSourceRootsRef.get();
        }
        return result;
    }

    private Map<String, JavaSourceSet> createNameToSourceSet() {
        Map<String, JavaSourceSet> result = CollectionUtils.newHashMap(sources.size());
        for (JavaSourceSet sourceSet: sources) {
            result.put(sourceSet.getName(), sourceSet);
        }
        return result;
    }

    private Map<String, JavaSourceSet> getNameToSourceSet() {
        Map<String, JavaSourceSet> result = nameToSourceSetRef.get();
        if (result == null) {
            nameToSourceSetRef.set(createNameToSourceSet());
            result = nameToSourceSetRef.get();
        }
        return result;
    }

    public JavaSourceSet tryGetSourceSetByName(String name) {
        ExceptionHelper.checkNotNullArgument(name, "name");

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
        Map<String, JavaTestTask> result = testNameToModelRef.get();
        if (result == null) {
            testNameToModelRef.set(createTestNameToModel());
            result = testNameToModelRef.get();
        }
        return result;
    }

    public JavaTestTask getTestModelByName(String name) {
        JavaTestTask testTask = tryGetTestModelByName(name);
        return testTask != null
                ? testTask
                : JavaTestTask.getDefaulTestModel(getModuleDir());
    }

    public JavaTestTask tryGetTestModelByName(String name) {
        ExceptionHelper.checkNotNullArgument(name, "name");

        return getTestNameToModel().get(name);
    }

    private Set<File> createAllBuildOutputs() {
        Set<File> result = CollectionUtils.newHashSet(sources.size());
        for (JavaSourceSet sourceSet: sources) {
            result.add(sourceSet.getOutputDirs().getClassesDir());
        }
        return Collections.unmodifiableSet(result);
    }

    public Set<File> getAllBuildOutputs() {
        Set<File> result = allBuildOutputRefs.get();
        if (result == null) {
            allBuildOutputRefs.set(createAllBuildOutputs());
            result = allBuildOutputRefs.get();
        }
        return result;
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
        ExceptionHelper.checkNotNullArgument(sourceSetName, "sourceSetName");

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
        private final JavaTestModel testTasks;
        private final NbCodeCoverage codeCoverage;

        public SerializedFormat(NbJavaModule source) {
            this.properties = source.properties;
            this.compatibilityModel = source.compatibilityModel;
            this.sources = source.sources;
            this.listedDirs = source.listedDirs;
            this.testTasks = source.testTasks;
            this.codeCoverage = source.codeCoverage;
        }

        private NbCodeCoverage getCodeCoverage() {
            return codeCoverage != null ? codeCoverage : NbCodeCoverage.NO_CODE_COVERAGE;
        }

        private Object readResolve() throws ObjectStreamException {
            return new NbJavaModule(
                    properties,
                    compatibilityModel,
                    sources,
                    listedDirs,
                    testTasks,
                    getCodeCoverage());
        }
    }

    private interface SourceSetFilter {
        public boolean needSourceSet(JavaSourceSet sourceSet);
        public boolean isPriority(JavaSourceSet sourceSet);
    }
}
