package org.netbeans.gradle.model.util;

import java.util.Collection;
import java.util.EnumMap;
import java.util.Map;
import org.netbeans.gradle.model.java.JavaOutputDirs;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceGroupName;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.model.java.JavaSourcesModel;

import static org.junit.Assert.*;

public final class SourceSetVerification {
    private static Map<String, JavaSourceSet> sourceSetMap(JavaSourcesModel sources) {
        Collection<JavaSourceSet> sourceSets = sources.getSourceSets();

        Map<String, JavaSourceSet> result = CollectionUtils.newHashMap(sourceSets.size());
        for (JavaSourceSet sourceSet: sourceSets) {
            result.put(sourceSet.getName(), sourceSet);
        }
        return result;
    }

    private static Map<JavaSourceGroupName, JavaSourceGroup> sourceGroupMap(
            Collection<JavaSourceGroup> sourceGroups) {

        Map<JavaSourceGroupName, JavaSourceGroup> result
                = new EnumMap<JavaSourceGroupName, JavaSourceGroup>(JavaSourceGroupName.class);

        for (JavaSourceGroup sourceGroup: sourceGroups) {
            result.put(sourceGroup.getGroupName(), sourceGroup);
        }
        return result;
    }

    public static void verifyOutputDirs(
            JavaOutputDirs expected,
            JavaOutputDirs actual) {

        assertEquals(expected.getClassesDirs(), actual.getClassesDirs());
        assertEquals(expected.getResourcesDir(), actual.getResourcesDir());
    }

    public static void verifySourceGroups(
            Collection<JavaSourceGroup> expected,
            Collection<JavaSourceGroup> actual) {

        Map<JavaSourceGroupName, JavaSourceGroup> expectedMap = sourceGroupMap(expected);

        for (JavaSourceGroup sourceGroup: actual) {
            JavaSourceGroupName name = sourceGroup.getGroupName();
            JavaSourceGroup expectedSourceGroup = expectedMap.get(name);
            expectedMap.remove(name);

            if (expectedSourceGroup == null) {
                throw new AssertionError("Unexpected source group: " + name);
            }

            assertEquals("Source groups (" + name + ") must contain the same source roots.",
                    expectedSourceGroup.getSourceRoots(), sourceGroup.getSourceRoots());
        }

        if (!expectedMap.isEmpty()) {
            fail("Missing expected source groups: " + expectedMap.keySet().toString());
        }
    }

    public static void verifySourceSetWithoutDependencies(
            JavaSourceSet expected,
            JavaSourceSet actual) {

        assertEquals("Source set names must match.", expected.getName(), actual.getName());
        verifyOutputDirs(expected.getOutputDirs(), actual.getOutputDirs());

        expected.getSourceGroups();
    }

    public static void verifySourcesModelWithoutDependencies(
            JavaSourcesModel expected,
            JavaSourcesModel actual) {

        Map<String, JavaSourceSet> expectedMap = sourceSetMap(expected);

        for (JavaSourceSet sourceSet: actual.getSourceSets()) {
            JavaSourceSet expectedSourceSet = expectedMap.get(sourceSet.getName());
            expectedMap.remove(sourceSet.getName());

            if (expectedSourceSet == null) {
                fail("Unexpected source set: " + sourceSet.getName());
            }

            verifySourceSetWithoutDependencies(expectedSourceSet, sourceSet);
        }

        if (!expectedMap.isEmpty()) {
            fail("Missing expected source sets: " + expectedMap.keySet().toString());
        }
    }

    private SourceSetVerification() {
        throw new AssertionError();
    }
}
