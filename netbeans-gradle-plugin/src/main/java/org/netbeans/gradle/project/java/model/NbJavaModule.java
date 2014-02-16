package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.tasks.TestTaskName;

public final class NbJavaModule implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GenericProjectProperties properties;
    private final JavaCompatibilityModel compatibilityModel;
    private final List<JavaSourceSet> sources;
    private final List<NbListedDir> listedDirs;
    private final JavaTestModel testTasks;

    private final AtomicReference<JavaSourceSet> mainSourceSetRef;
    private final AtomicReference<JavaSourceSet> testSourceSetRef;
    private final AtomicReference<List<JavaSourceSet>> testSourceSets;
    private final AtomicReference<List<NamedSourceRoot>> namedSourceRootsRef;
    private final AtomicReference<Map<String, JavaSourceSet>> nameToSourceSetRef;
    private final AtomicReference<Map<String, JavaTestTask>> testNameToModelRef;
    private final AtomicReference<Set<File>> allBuildOutputRefs;

    public NbJavaModule(
            GenericProjectProperties properties,
            JavaCompatibilityModel compatibilityModel,
            Collection<JavaSourceSet> sources,
            List<NbListedDir> listedDirs,
            JavaTestModel testTasks) {

        if (properties == null) throw new NullPointerException("properties");
        if (compatibilityModel == null) throw new NullPointerException("compatibilityModel");
        if (sources == null) throw new NullPointerException("sources");
        if (listedDirs == null) throw new NullPointerException("listedDirs");
        if (testTasks == null) throw new NullPointerException("testTasks");

        this.properties = properties;
        this.compatibilityModel = compatibilityModel;
        this.sources = CollectionUtils.copyNullSafeList(sources);
        this.listedDirs = CollectionUtils.copyNullSafeList(listedDirs);
        this.testTasks = testTasks;

        this.mainSourceSetRef = new AtomicReference<JavaSourceSet>(null);
        this.testSourceSetRef = new AtomicReference<JavaSourceSet>(null);
        this.testSourceSets = new AtomicReference<List<JavaSourceSet>>(null);
        this.namedSourceRootsRef = new AtomicReference<List<NamedSourceRoot>>(null);
        this.nameToSourceSetRef = new AtomicReference<Map<String, JavaSourceSet>>(null);
        this.testNameToModelRef = new AtomicReference<Map<String, JavaTestTask>>(null);
        this.allBuildOutputRefs = new AtomicReference<Set<File>>(null);
    }

    public GenericProjectProperties getProperties() {
        return properties;
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

    private List<JavaSourceSet> findTestSourceSets() {
        List<JavaSourceSet> result = new ArrayList<JavaSourceSet>(Math.max(sources.size() - 1, 0));
        for (JavaSourceSet sourceSet: sources) {
            if (!JavaSourceGroupID.isTestSourceSet(sourceSet.getName())) {
                continue;
            }

            if (JavaSourceSet.NAME_TEST.equals(sourceSet.getName())) {
                result.add(0, sourceSet);
            }
            else {
                result.add(sourceSet);
            }
        }
        return result;
    }

    public List<JavaSourceSet> getTestSourceSets() {
        List<JavaSourceSet> result = testSourceSets.get();
        if (result == null) {
            testSourceSets.set(findTestSourceSets());
            result = testSourceSets.get();
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
        if (name == null) throw new NullPointerException("name");

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

    public JavaTestTask tryGetTestModelByName(String name) {
        if (name == null) throw new NullPointerException("name");

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
        if (sourceSetName == null) throw new NullPointerException("sourceSetName");

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

        public SerializedFormat(NbJavaModule source) {
            this.properties = source.properties;
            this.compatibilityModel = source.compatibilityModel;
            this.sources = source.sources;
            this.listedDirs = source.listedDirs;
            this.testTasks = source.testTasks;
        }

        private Object readResolve() throws ObjectStreamException {
            return new NbJavaModule(properties, compatibilityModel, sources, listedDirs, testTasks);
        }
    }
}
