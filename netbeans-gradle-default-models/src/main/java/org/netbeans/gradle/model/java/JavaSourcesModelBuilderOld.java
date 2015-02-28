package org.netbeans.gradle.model.java;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.util.BuilderUtils;

import static org.netbeans.gradle.model.util.ReflectionUtils.*;

/**
 * Defines a {@code ProjectInfoBuilder} which is able to extract
 * {@link JavaSourcesModel} from a Gradle project.
 * <P>
 * This builder has only two instances {@link #ONLY_COMPILE} and
 * {@link #COMPLETE}.
 */
public enum JavaSourcesModelBuilderOld
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

    private JavaSourcesModelBuilderOld(boolean needRuntime) {
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

        return new Builder(project, needRuntime).getProjectInfo();
    }

    /** {@inheritDoc } */
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }

    private static final class Builder {
        private final Project project;
        private final boolean needRuntime;

        public Builder(Project project, boolean needRuntime) {
            this.project = project;
            this.needRuntime = needRuntime;
        }

        public JavaSourcesModel getProjectInfo() {
            List<JavaSourceSet> result = new LinkedList<JavaSourceSet>();

            Iterable<?> sourceSets = (Iterable<?>)project.property("sourceSets");
            for (Object sourceSet: sourceSets) {
                result.add(parseSourceSet(sourceSet));
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

        private JavaSourceSet parseSourceSet(Object sourceSet) {
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

            parseClassPaths(sourceSet, result);

            return result.create();
        }

        @SuppressWarnings("unchecked")
        private Collection<? extends File> resolveDependencies(Object sourceSet, String dependencyName) {
            return (Collection<? extends File>)getNonBoolProperty(
                    getNonBoolProperty(sourceSet, dependencyName),
                    "files");
        }

        private void parseClassPaths(Object sourceSet, JavaSourceSet.Builder result) {
            Collection<? extends File> compile = Collections.emptySet();
            boolean compileResolved = false;
            try {
                compile = resolveDependencies(sourceSet, "compileClasspath");
                compileResolved = true;
            } catch (Throwable ex) {
                result.setCompileClassPathProblem(ex);
            }

            if (!needRuntime) {
                result.setClasspaths(new JavaClassPaths(compile));
                return;
            }

            Collection<? extends File> runtime = compile;
            try {
                runtime = resolveDependencies(sourceSet, "runtimeClasspath");
                if (!compileResolved) {
                    compile = runtime;
                }
            } catch (Throwable ex) {
                result.setRuntimeClassPathProblem(ex);
            }

            result.setClasspaths(new JavaClassPaths(compile, runtime));
        }

        private JavaSourceGroup parseSourceGroup(JavaSourceGroupName name, Object sourceGroup) {
            @SuppressWarnings("unchecked")
            Collection<? extends File> srcDirs = (Collection<? extends File>)getNonBoolProperty(sourceGroup, "srcDirs");

            @SuppressWarnings("unchecked")
            Collection<? extends String> includes = (Collection<? extends String>)getNonBoolProperty(sourceGroup, "includes");

            @SuppressWarnings("unchecked")
            Collection<? extends String> excludes = (Collection<? extends String>)getNonBoolProperty(sourceGroup, "excludes");

            return new JavaSourceGroup(name, srcDirs, excludes, includes);
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
}
