package org.netbeans.gradle.model.java;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.file.SourceDirectorySet;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.plugins.scala.ScalaPlugin;
import org.gradle.api.specs.Specs;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.gradle.api.tasks.SourceSetOutput;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.BuilderUtils;
import org.netbeans.gradle.model.util.Exceptions;

/**
 * Defines a {@code ProjectInfoBuilder2} which is able to extract
 * {@link JavaSourcesModel} from a Gradle project.
 * <P>
 * This builder has only two instances {@link #ONLY_COMPILE} and
 * {@link #COMPLETE}.
 */
enum JavaSourcesModelBuilder
implements
        ProjectInfoBuilder2<JavaSourcesModel> {

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
    @Override
    public JavaSourcesModel getProjectInfo(Object project) {
        return getProjectInfo((Project)project);
    }

    private JavaSourcesModel getProjectInfo(Project project) {
        JavaPluginConvention javaPlugin = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaPlugin == null) {
            return null;
        }

        try {
            return new Builder(project, javaPlugin, needRuntime).getProjectInfo();
        } catch (Exception ex) {
            throw Exceptions.throwUnchecked(ex);
        }
    }

    /** {@inheritDoc } */
    @Override
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }

    private static final class Builder {
        private final Project project;
        private final JavaPluginConvention javaPlugin;
        private final boolean needRuntime;

        public Builder(Project project, JavaPluginConvention javaPlugin, boolean needRuntime) throws Exception {
            this.project = project;
            this.javaPlugin = javaPlugin;
            this.needRuntime = needRuntime;
        }

        public JavaSourcesModel getProjectInfo() throws Exception {
            List<JavaSourceSet> result = new LinkedList<JavaSourceSet>();
            SourceSetContainer sourceSets = javaPlugin.getSourceSets();
            for (SourceSet sourceSet: sourceSets) {
                result.add(parseSourceSet(sourceSet));
            }

            return new JavaSourcesModel(result);
        }

        private Collection<File> addSourceGroup(
                JavaSourceGroupName name,
                SourceDirectorySet sourceGroup,
                JavaSourceSet.Builder result) throws Exception {

            JavaSourceGroup parsedGroup = parseSourceGroup(name, sourceGroup);
            result.addSourceGroup(parsedGroup);
            return parsedGroup.getSourceRoots();
        }

        private JavaSourceSet parseSourceSet(SourceSet sourceSet) throws Exception {
            JavaOutputDirs outputDirs = parseOutputDirs(sourceSet.getOutput());
            JavaSourceSet.Builder result = new JavaSourceSet.Builder(
                    sourceSet.getName(),
                    outputDirs);

            SourceDirectorySet allSourceGroup = sourceSet.getAllSource();
            Set<File> others = new HashSet<File>(allSourceGroup.getSrcDirs());

            others.removeAll(addSourceGroup(JavaSourceGroupName.JAVA, sourceSet.getJava(), result));

            if (project.getPlugins().hasPlugin(GroovyPlugin.class)) {
                others.removeAll(addSourceGroup(JavaSourceGroupName.GROOVY, JavaSourcesUtils.getGroovySources(sourceSet), result));
            }

            if (project.getPlugins().hasPlugin(ScalaPlugin.class)) {
                others.removeAll(addSourceGroup(JavaSourceGroupName.SCALA, JavaSourcesUtils.getScalaSources(sourceSet), result));
            }

            others.removeAll(addSourceGroup(JavaSourceGroupName.RESOURCES, sourceSet.getResources(), result));

            result.addSourceGroup(new JavaSourceGroup(JavaSourceGroupName.OTHER, others));

            parseClassPaths(sourceSet, result);

            return result.create();
        }

        private Set<File> getLenientClasspath(String configName, Throwable baseError) {
            try {
                Configuration config = project.getConfigurations().findByName(configName);
                if (config != null) {
                    return config.getResolvedConfiguration().getLenientConfiguration().getFiles(Specs.SATISFIES_ALL);
                }
            } catch (Throwable ex) {
                Exceptions.tryAddSuppressedException(baseError, ex);
            }

            return Collections.emptySet();
        }

        private void parseClassPaths(SourceSet sourceSet, JavaSourceSet.Builder result) {
            Set<File> compile;
            boolean compileResolved = false;
            try {
                compile = sourceSet.getCompileClasspath().getFiles();
                compileResolved = true;
            } catch (Throwable ex) {
                result.setCompileClassPathProblem(ex);
                compile = getLenientClasspath(sourceSet.getCompileConfigurationName(), ex);
            }

            if (!needRuntime) {
                result.setClasspaths(new JavaClassPaths(compile));
                return;
            }

            Set<File> runtime = compile;
            try {
                runtime = sourceSet.getRuntimeClasspath().getFiles();
                if (!compileResolved) {
                    compile = runtime;
                }
            } catch (Throwable ex) {
                result.setRuntimeClassPathProblem(ex);
            }

            result.setClasspaths(new JavaClassPaths(compile, runtime));
        }

        private JavaSourceGroup parseSourceGroup(JavaSourceGroupName name, SourceDirectorySet sourceGroup) throws Exception {
            Set<File> srcDirs = sourceGroup.getSrcDirs();
            Set<String> excludes = sourceGroup.getExcludes();
            Set<String> includes = sourceGroup.getIncludes();
            SourceIncludePatterns patterns = SourceIncludePatterns.create(excludes, includes);
            return new JavaSourceGroup(name, srcDirs, patterns);
        }

        private JavaOutputDirs parseOutputDirs(SourceSetOutput outputDirs) throws Exception {
            Set<File> classesDirs = JavaSourcesUtils.getClassesDirs(outputDirs);
            File resourcesDir = outputDirs.getResourcesDir();
            Set<File> otherDirs = outputDirs.getDirs().getFiles();

            return new JavaOutputDirs(classesDirs, resourcesDir, otherDirs);
        }
    }
}
