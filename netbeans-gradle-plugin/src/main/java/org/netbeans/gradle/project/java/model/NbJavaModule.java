package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceSet;

public final class NbJavaModule {
    private final GenericProjectProperties properties;
    private final JavaCompatibilityModel compatibilityModel;
    private final List<JavaSourceSet> sources;
    private final List<File> listedDirs;

    private final AtomicReference<JavaSourceSet> mainSourceSetRef;
    private final AtomicReference<JavaSourceSet> testSourceSetRef;
    private final AtomicReference<List<NamedSourceRoot>> namedSourceRootsRef;

    public NbJavaModule(
            GenericProjectProperties properties,
            JavaCompatibilityModel compatibilityModel,
            Collection<JavaSourceSet> sources,
            List<File> listedDirs) {

        if (properties == null) throw new NullPointerException("properties");
        if (compatibilityModel == null) throw new NullPointerException("compatibilityModel");
        if (sources == null) throw new NullPointerException("sources");
        if (listedDirs == null) throw new NullPointerException("listedDirs");

        this.properties = properties;
        this.compatibilityModel = compatibilityModel;
        this.sources = Collections.unmodifiableList(new ArrayList<JavaSourceSet>(sources));
        this.listedDirs = Collections.unmodifiableList(listedDirs);

        this.mainSourceSetRef = new AtomicReference<JavaSourceSet>(null);
        this.testSourceSetRef = new AtomicReference<JavaSourceSet>(null);
        this.namedSourceRootsRef = new AtomicReference<List<NamedSourceRoot>>(null);
    }

    public GenericProjectProperties getProperties() {
        return properties;
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

    public List<File> getListedDirs() {
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

    public List<NamedSourceRoot> getNamedSourceRoots() {
        List<NamedSourceRoot> result = namedSourceRootsRef.get();
        if (result == null) {
            namedSourceRootsRef.set(Collections.unmodifiableList(NamedSourceRoot.getAllSourceRoots(this)));
            result = namedSourceRootsRef.get();
        }
        return result;
    }
}
