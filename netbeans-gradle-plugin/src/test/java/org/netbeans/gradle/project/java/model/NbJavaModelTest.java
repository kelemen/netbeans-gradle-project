package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.junit.Test;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.util.SerializationCache;
import org.netbeans.gradle.model.util.SerializationUtils;
import org.netbeans.gradle.project.model.NbGradleProjectTreeTest;
import org.netbeans.gradle.project.util.GradleVersions;

import static org.junit.Assert.*;

public class NbJavaModelTest {
    private static JavaSourceGroup createSourceGroup(String name) {
        return new JavaSourceGroup(JavaSourceGroupName.JAVA, Arrays.asList(new File(name + "-src-java")));
    }

    private static JavaSourceSet createSources(String name) {
        JavaSourceSet.Builder result = new JavaSourceSet.Builder(
                name,
                new JavaOutputDirs(Collections.singleton(new File(name + "-out-classes")), new File(name + "-out-res"), Collections.<File>emptySet()));
        result.addSourceGroup(createSourceGroup(name));
        return result.create();
    }

    private static NbJavaModule createModule(String name) {
        String fullName = ":apps:" + name;
        GenericProjectProperties properties = NbGradleProjectTreeTest.createProperties(name, fullName);
        JavaCompatibilityModel compatibility = new JavaCompatibilityModel("1.6", "1.7");
        List<JavaSourceSet> sources = Arrays.asList(createSources("main"), createSources("test"));
        List<NbListedDir> listedDirs = Arrays.asList(
                new NbListedDir("my-listed-dir", new File("listed-dir")));
        JavaTestModel testModel = JavaTestModel.getDefaulTestModel(properties.getProjectDir());

        List<NbJarOutput> jarOutputs = Arrays.asList(
                new NbJarOutput("jar",
                        new File(properties.getBuildDir(), "myproject.jar"),
                        sources.get(0).getOutputDirs().getClassesDirs())
        );

        return new NbJavaModule(properties, compatibility, sources, listedDirs, jarOutputs, testModel, NbCodeCoverage.NO_CODE_COVERAGE);
    }

    @Test
    public void testSerialization() throws ClassNotFoundException {
        NbJavaModule mainModule = createModule("mainModule");
        NbJavaModel source = NbJavaModel.createModel(
                GradleVersions.DEFAULT_TARGET,
                JavaModelSource.GRADLE_1_8_API,
                mainModule);


        byte[] serialized = SerializationUtils.serializeObject(source);
        NbJavaModel deserialized = (NbJavaModel)SerializationUtils.deserializeObject(serialized, SerializationCache.NO_CACHE);

        assertEquals(
                source.getMainModule().getModuleDir().toString(),
                deserialized.getMainModule().getModuleDir().toString());
    }
}
