package org.netbeans.gradle.model.java;

import org.gradle.api.*;
import org.netbeans.gradle.model.ProjectInfoBuilder;

/**
 * Defines a {@code ProjectInfoBuilder} which is able to extract
 * {@link JavaSourcesModel} from a Gradle project.
 * <P>
 * Since this builder does not have any input argument, it is singleton and its
 * instance can be accessed through {@code JavaSourcesModelBuilder.INSTANCE}.
 */
public enum JavaSourcesModelBuilder
implements
        ProjectInfoBuilder<JavaSourcesModel> {

    /**
     * The one and only instance of {@code JavaSourcesModelBuilder}.
     */
    INSTANCE;

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
        if (!project.plugins.hasPlugin('java')) {
            return null;
        }

        List<JavaSourceSet> result = new LinkedList();

        project.sourceSets.each {
            result.add(parseSourceSet(project, it));
        }

        return new JavaSourcesModel(result);
    }

    private static boolean implementsInterface(def obj, String ifName) {
        try {
            Class<?> cl = Class.forName('org.gradle.api.tasks.GroovySourceSet');
            return cl.isAssignableFrom(obj.getClass());
        } catch (ClassNotFoundException ex) {
            return false;
        }
    }

    private Collection<File> addSourceGroup(
            JavaSourceGroupName name,
            def sourceGroup,
            JavaSourceSet.Builder result) {

        JavaSourceGroup parsedGroup = parseSourceGroup(name, sourceGroup);
        result.addSourceGroup(parsedGroup);
        return parsedGroup.sourceRoots;
    }

    private JavaSourceSet parseSourceSet(Project project, def sourceSet) {
        def outputDirs = parseOutputDirs(sourceSet.output);
        JavaSourceSet.Builder result = new JavaSourceSet.Builder(sourceSet.name, outputDirs);

        Set<File> others = new HashSet(sourceSet.allSource.srcDirs);

        others.removeAll(addSourceGroup(JavaSourceGroupName.JAVA, sourceSet.java, result));

        if (project.plugins.hasPlugin('groovy')) {
            others.removeAll(addSourceGroup(JavaSourceGroupName.GROOVY, sourceSet.groovy, result));
        }

        if (project.plugins.hasPlugin('scala')) {
            others.removeAll(addSourceGroup(JavaSourceGroupName.SCALA, sourceSet.scala, result));
        }

        others.removeAll(addSourceGroup(JavaSourceGroupName.RESOURCES, sourceSet.resources, result));

        result.addSourceGroup(new JavaSourceGroup(JavaSourceGroupName.OTHER, others));

        result.setClasspaths(parseClassPaths(sourceSet));

        return result.create();
    }

    private JavaClassPaths parseClassPaths(def sourceSet) {
        def compile = sourceSet.compileClasspath.files;
        def runtime = sourceSet.runtimeClasspath.files;

        return new JavaClassPaths(compile, runtime);
    }

    private JavaSourceGroup parseSourceGroup(JavaSourceGroupName name, def sourceGroup) {
        return new JavaSourceGroup(name, sourceGroup.srcDirs);
    }

    private JavaOutputDirs parseOutputDirs(def outputDirs) {
        List<File> otherDirs = new LinkedList();
        outputDirs.dirs.files.each {
            otherDirs.add(it);
        }

        return new JavaOutputDirs(outputDirs.classesDir, outputDirs.resourcesDir, otherDirs);
    }
}

