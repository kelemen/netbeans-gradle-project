package org.netbeans.gradle.model.java;

import java.io.File;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.gradle.api.*;
import org.netbeans.gradle.model.ProjectInfoBuilder;

import static org.netbeans.gradle.model.util.ReflectionUtils.*;

/**
 * Defines a {@code ProjectInfoBuilder} which is able to extract
 * {@link JavaSourcesModel} from a Gradle project.
 * <P>
 * This builder has only two instances {@link #ONLY_COMPILE} and
 * {@link #COMPLETE}.
 */
public enum JavaSourcesModelBuilder
implements
        ProjectInfoBuilder<JavaSourcesModel> {

    /**
     * The builder instance which will not attempt to query runtime dependencies.
     */
    ONLY_COMPILE(false),

    /**
     * The builder instance which will request both runtime and compile time
     * dependencies.
     */
    COMPLETE(true);

    private final boolean needRuntime;

    private JavaSourcesModelBuilder(boolean needRuntime) {
        this.needRuntime = needRuntime;
    }

    /**
     *Extracts and returns the {@code JavaSourcesModel} from the given
     * project or returns {@code null} if the project does not uses the 'java'
     * plugin.
     *
     * @param project the project from which the information is to be extracted.
     *   This argument cannot be {@code null}.
     *
     * @return the {@code JavaSourcesModel} extracted from the given
     *   project or {@code null} if the project does not applies the "java"
     *   plugin
     */
    public JavaSourcesModel getProjectInfo(Project project) {
        if (!project.getPlugins().hasPlugin("java")) {
            return null;
        }

        List<JavaSourceSet> result = new LinkedList<JavaSourceSet>();

        Iterable<?> sourceSets = (Iterable<?>)project.property("sourceSets");
        for (Object sourceSet: sourceSets) {
            result.add(parseSourceSet(project, sourceSet));
        }

        return new JavaSourcesModel(result);
    }

    private Collection<File> addSourceGroupNonDyn(
            JavaSourceGroupName name,
            Object sourceSet,
            String groupName,
            JavaSourceSet.Builder result) {
        Object sourceGroup = getNonBoolProperty(sourceSet, groupName);
        return addSourceGroup(name, sourceGroup, result);
    }

    private Collection<File> addSourceGroup(
            JavaSourceGroupName name,
            Object sourceSet,
            String groupName,
            JavaSourceSet.Builder result) {
        Object sourceGroup = getNonBoolPropertyDyn(sourceSet, groupName);
        return addSourceGroup(name, sourceGroup, result);
    }

    private Collection<File> addSourceGroup(
            JavaSourceGroupName name,
            Object sourceGroup,
            JavaSourceSet.Builder result) {

        JavaSourceGroup parsedGroup = parseSourceGroup(name, sourceGroup);
        result.addSourceGroup(parsedGroup);
        return parsedGroup.getSourceRoots();
    }

    private JavaSourceSet parseSourceSet(Project project, Object sourceSet) {
        JavaOutputDirs outputDirs = parseOutputDirs(getNonBoolProperty(sourceSet, "output"));
        JavaSourceSet.Builder result = new JavaSourceSet.Builder(
                getStringProperty(sourceSet, "name"),
                outputDirs);

        Object allSourceGroup = getNonBoolProperty(sourceSet, "allSource");

        @SuppressWarnings("unchecked")
        Collection<? extends File> allSourceGroupFiles =
                (Collection<? extends File>)getNonBoolProperty(allSourceGroup, "srcDirs");

        Set<File> others = new HashSet<File>(allSourceGroupFiles);

        others.removeAll(addSourceGroupNonDyn(JavaSourceGroupName.JAVA, sourceSet, "java", result));

        if (project.getPlugins().hasPlugin("groovy")) {
            others.removeAll(addSourceGroup(JavaSourceGroupName.GROOVY, sourceSet, "groovy", result));
        }

        if (project.getPlugins().hasPlugin("scala")) {
            others.removeAll(addSourceGroup(JavaSourceGroupName.SCALA, sourceSet, "scala", result));
        }

        others.removeAll(addSourceGroupNonDyn(JavaSourceGroupName.RESOURCES, sourceSet, "resources", result));

        result.addSourceGroup(new JavaSourceGroup(JavaSourceGroupName.OTHER, others));

        result.setClasspaths(parseClassPaths(sourceSet));

        return result.create();
    }

    @SuppressWarnings("unchecked")
    private JavaClassPaths parseClassPaths(Object sourceSet) {
        Collection<? extends File> compile = (Collection<? extends File>)getNonBoolProperty(
                getNonBoolProperty(sourceSet, "compileClasspath"), "files");

        if (!needRuntime) {
            return new JavaClassPaths(compile);
        }

        Collection<? extends File> runtime = (Collection<? extends File>)getNonBoolProperty(
                getNonBoolProperty(sourceSet, "runtimeClasspath"), "files");

        return new JavaClassPaths(compile, runtime);
    }

    private JavaSourceGroup parseSourceGroup(JavaSourceGroupName name, Object sourceGroup) {
        @SuppressWarnings("unchecked")
        Collection<? extends File> srcDirs = (Collection<? extends File>)getNonBoolProperty(sourceGroup, "srcDirs");
        return new JavaSourceGroup(name, srcDirs);
    }

    private JavaOutputDirs parseOutputDirs(Object outputDirs) {
        List<File> otherDirs = new LinkedList<File>();

        @SuppressWarnings("unchecked")
        Iterable<? extends File> files = (Iterable<? extends File>)getNonBoolProperty(
                getNonBoolProperty(outputDirs, "dirs"), "files");

        for (File file: files) {
            otherDirs.add(file);
        }

        File classesDir = (File)getNonBoolProperty(outputDirs, "classesDir");
        File resourcesDir = (File)getNonBoolProperty(outputDirs, "resourcesDir");

        return new JavaOutputDirs(classesDir, resourcesDir, otherDirs);
    }
}
