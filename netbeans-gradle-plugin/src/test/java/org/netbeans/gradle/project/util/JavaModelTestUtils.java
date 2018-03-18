package org.netbeans.gradle.project.util;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.netbeans.gradle.model.GenericProjectProperties;
import org.netbeans.gradle.model.java.JavaCompatibilityModel;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaTestModel;
import org.netbeans.gradle.model.java.JavaTestTask;
import org.netbeans.gradle.model.util.BasicFileUtils;
import org.netbeans.gradle.project.java.model.NbCodeCoverage;
import org.netbeans.gradle.project.java.model.NbJarOutput;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.java.model.NbListedDir;
import org.netbeans.gradle.project.script.CommonScripts;

public final class JavaModelTestUtils {
    private static JavaSourceSet createDefaultSourceSet(File rootDir, String name) throws IOException {
        File srcRoot = BasicFileUtils.getSubPath(rootDir, "src", name, "java");
        File srcClassDir = BasicFileUtils.getSubPath(srcRoot, "testpckg", name);
        srcClassDir.mkdirs();

        String packageName = "testpckg." + name;
        String className = "TestClass" + name;
        File srcClassFile = new File(srcClassDir, className + ".java");

        List<String> lines = Arrays.asList(
                "package " + packageName + ";",
                "public class " + className + " { }");
        Files.write(srcClassFile.toPath(), lines, StandardCharsets.UTF_8);

        File buildDir = new File(rootDir, "build");
        File classesDir = new File(buildDir, "classes");
        File resourcesDir = new File(buildDir, "resources");

        JavaSourceSet.Builder result = new JavaSourceSet.Builder(name, new JavaOutputDirs(
                Collections.singleton(new File(classesDir, name)),
                new File(resourcesDir, name),
                Collections.<File>emptyList()));
        result.addSourceGroup(new JavaSourceGroup(JavaSourceGroupName.JAVA, Collections.singletonList(srcRoot)));

        return result.create();
    }

    private static List<NbJarOutput> createJarOutputs(File rootDir, Collection<JavaSourceSet> sources) {
        File buildDir = new File(rootDir, "build");
        File jarDir = new File(buildDir, "jars");

        List<NbJarOutput> result = new ArrayList<>(sources.size());
        for (JavaSourceSet sourceSet: sources) {
            File jar = new File(jarDir, sourceSet.getName() + ".jar");
            Set<File> classesDirs = sourceSet.getOutputDirs().getClassesDirs();
            result.add(new NbJarOutput(sourceSet.getName() + "Jar", jar, classesDirs));
        }
        return result;
    }

    public static NbJavaModule createModule(File rootDir) throws IOException {
        GenericProjectProperties properties = new GenericProjectProperties(
                "testProject",
                ":parent:testProject",
                rootDir,
                new File(rootDir, CommonScripts.BUILD_BASE_NAME + CommonScripts.DEFAULT_SCRIPT_EXTENSION));
        JavaCompatibilityModel compatibilityModel = new JavaCompatibilityModel("1.8", "1.8");
        Collection<JavaSourceSet> sources = Arrays.asList(
                createDefaultSourceSet(rootDir, "main"),
                createDefaultSourceSet(rootDir, "customSourceSet"),
                createDefaultSourceSet(rootDir, "test"));
        List<NbListedDir> listedDirs = Collections.emptyList();
        List<NbJarOutput> jarOutputs = createJarOutputs(rootDir, sources);
        JavaTestModel testTasks = new JavaTestModel(Collections.<JavaTestTask>emptyList());

        return new NbJavaModule(
                properties,
                compatibilityModel,
                sources,
                listedDirs,
                jarOutputs,
                testTasks,
                NbCodeCoverage.NO_CODE_COVERAGE);
    }

    public static Set<File> getAllSourceDirs(JavaSourceSet sourceSet) {
        Set<File> result = new HashSet<>();
        for (JavaSourceGroup group: sourceSet.getSourceGroups()) {
            result.addAll(group.getSourceRoots());
        }
        return result;
    }

    private JavaModelTestUtils() {
        throw new AssertionError();
    }
}
