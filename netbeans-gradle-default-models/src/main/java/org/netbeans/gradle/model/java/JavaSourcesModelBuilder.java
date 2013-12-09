package org.netbeans.gradle.model.java;

import java.io.File;
import java.lang.reflect.Method;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.file.FileCollection;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.gradleclasses.GradleClass;
import org.netbeans.gradle.model.gradleclasses.GradleClasses;
import org.netbeans.gradle.model.util.BuilderUtils;
import org.netbeans.gradle.model.util.Exceptions;

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
        try {
            return new Builder(project, needRuntime).getProjectInfo();
        } catch (Exception ex) {
            throw Exceptions.throwUnchecked(ex);
        }
    }

    /** {@inheritDoc } */
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }

    private static final class SourceDirectorySetMethods {
        private static volatile SourceDirectorySetMethods CACHE = null;

        private final GradleClass type;
        private final Method getSrcDirs;

        public SourceDirectorySetMethods(GradleClass type) throws Exception {
            this.type = type;
            this.getSrcDirs = type.getMethod("getSrcDirs");
        }

        public static SourceDirectorySetMethods getInstance(Project project) throws Exception {
            SourceDirectorySetMethods result = CACHE;
            GradleClass type = GradleClasses.getGradleClass(project, "org.gradle.api.file.SourceDirectorySet");
            if (result != null && type.equals(result.type)) {
                return result;
            }
            result = new SourceDirectorySetMethods(type);
            CACHE = result;
            return result;
        }

        @SuppressWarnings("unchecked")
        public Set<File> getSrcDirs(Object sourceDirectorySet) throws Exception {
            return (Set<File>)getSrcDirs.invoke(sourceDirectorySet);
        }
    }

    private static final class SourceSetOutputMethods {
        private static volatile SourceSetOutputMethods CACHE = null;

        private final GradleClass type;
        private final Method getDirs;
        private final Method getClassesDir;
        private final Method getResourcesDir;

        public SourceSetOutputMethods(GradleClass type) throws NoSuchMethodException {
            this.type = type;
            this.getDirs = type.getMethod("getDirs");
            this.getClassesDir = type.getMethod("getClassesDir");
            this.getResourcesDir = type.getMethod("getResourcesDir");
        }

        public static SourceSetOutputMethods getInstance(Project project) throws Exception {
            SourceSetOutputMethods result = CACHE;
            GradleClass type = GradleClasses.getGradleClass(project, "org.gradle.api.tasks.SourceSetOutput");
            if (result != null && type.equals(result.type)) {
                return result;
            }
            result = new SourceSetOutputMethods(type);
            CACHE = result;
            return result;
        }

        public FileCollection getDirs(Object sourceSetOutput) throws Exception {
            return (FileCollection)getDirs.invoke(sourceSetOutput);
        }

        public File getClassesDir(Object sourceSetOutput) throws Exception {
            return (File)getClassesDir.invoke(sourceSetOutput);
        }

        public File getResourcesDir(Object sourceSetOutput) throws Exception {
            return (File)getResourcesDir.invoke(sourceSetOutput);
        }
    }

    private static final class SourceSetMethods {
        private static volatile SourceSetMethods CACHE = null;

        private final GradleClass type;
        private final Method getOutput;
        private final Method getName;
        private final Method getJava;
        private final Method getResources;
        private final Method getAllSource;
        private final Method getCompileClasspath;
        private final Method getRuntimeClasspath;

        private SourceSetMethods(GradleClass type) throws Exception {
            this.type = type;
            this.getOutput = type.getMethod("getOutput");
            this.getName = type.getMethod("getName");
            this.getJava = type.getMethod("getJava");
            this.getResources = type.getMethod("getResources");
            this.getAllSource = type.getMethod("getAllSource");
            this.getCompileClasspath = type.getMethod("getCompileClasspath");
            this.getRuntimeClasspath = type.getMethod("getRuntimeClasspath");
        }

        public static SourceSetMethods getInstance(Project project) throws Exception {
            SourceSetMethods result = CACHE;
            GradleClass type = GradleClasses.getGradleClass(project, "org.gradle.api.tasks.SourceSet");
            if (result != null && type.equals(result.type)) {
                return result;
            }
            result = new SourceSetMethods(type);
            CACHE = result;
            return result;
        }

        public Object getOutput(Object sourceSet) throws Exception {
            return getOutput.invoke(sourceSet);
        }

        public String getName(Object sourceSet) throws Exception {
            Object result = getName.invoke(sourceSet);
            return result != null ? result.toString() : null;
        }

        public Object getJava(Object sourceSet) throws Exception {
            return getJava.invoke(sourceSet);
        }

        public Object getResources(Object sourceSet) throws Exception {
            return getResources.invoke(sourceSet);
        }

        public Object getAllSource(Object sourceSet) throws Exception {
            return getAllSource.invoke(sourceSet);
        }

        public FileCollection getCompileClasspath(Object sourceSet) throws Exception {
            return (FileCollection)getCompileClasspath.invoke(sourceSet);
        }

        public FileCollection getRuntimeClasspath(Object sourceSet) throws Exception {
            return (FileCollection)getRuntimeClasspath.invoke(sourceSet);
        }
    }

    private static final class Builder {
        private final Project project;
        private final boolean needRuntime;

        private final SourceSetMethods sourceSetMethods;
        private final SourceDirectorySetMethods sourceDirectorySetMethods;
        private final SourceSetOutputMethods sourceSetOutputMethods;

        public Builder(Project project, boolean needRuntime)
                throws Exception {

            this.project = project;
            this.needRuntime = needRuntime;
            this.sourceSetMethods = SourceSetMethods.getInstance(project);
            this.sourceDirectorySetMethods = SourceDirectorySetMethods.getInstance(project);
            this.sourceSetOutputMethods = SourceSetOutputMethods.getInstance(project);
        }

        public JavaSourcesModel getProjectInfo() throws Exception {
            List<JavaSourceSet> result = new LinkedList<JavaSourceSet>();

            Iterable<?> sourceSets = (Iterable<?>)project.property("sourceSets");
            for (Object sourceSet: sourceSets) {
                result.add(parseSourceSet(sourceSet));
            }

            return new JavaSourcesModel(result);
        }

        private Collection<File> addSourceGroup(
                JavaSourceGroupName name,
                Object sourceSet,
                String groupName,
                JavaSourceSet.Builder result) throws Exception {
            Object sourceGroup = getNonBoolPropertyDyn(sourceSet, groupName);
            return addSourceGroup(name, sourceGroup, result);
        }

        private Collection<File> addSourceGroup(
                JavaSourceGroupName name,
                Object sourceGroup,
                JavaSourceSet.Builder result) throws Exception {

            JavaSourceGroup parsedGroup = parseSourceGroup(name, sourceGroup);
            result.addSourceGroup(parsedGroup);
            return parsedGroup.getSourceRoots();
        }

        private JavaSourceSet parseSourceSet(Object sourceSet) throws Exception {
            JavaOutputDirs outputDirs = parseOutputDirs(sourceSetMethods.getOutput(sourceSet));
            JavaSourceSet.Builder result = new JavaSourceSet.Builder(
                    sourceSetMethods.getName(sourceSet),
                    outputDirs);

            Object allSourceGroup = sourceSetMethods.getAllSource(sourceSet);

            Collection<? extends File> allSourceGroupFiles
                    = sourceDirectorySetMethods.getSrcDirs(allSourceGroup);

            Set<File> others = new HashSet<File>(allSourceGroupFiles);

            others.removeAll(addSourceGroup(JavaSourceGroupName.JAVA, sourceSetMethods.getJava(sourceSet), result));

            if (project.getPlugins().hasPlugin("groovy")) {
                others.removeAll(addSourceGroup(JavaSourceGroupName.GROOVY, sourceSet, "groovy", result));
            }

            if (project.getPlugins().hasPlugin("scala")) {
                others.removeAll(addSourceGroup(JavaSourceGroupName.SCALA, sourceSet, "scala", result));
            }

            others.removeAll(addSourceGroup(JavaSourceGroupName.RESOURCES, sourceSetMethods.getResources(sourceSet), result));

            result.addSourceGroup(new JavaSourceGroup(JavaSourceGroupName.OTHER, others));

            parseClassPaths(sourceSet, result);

            return result.create();
        }

        private void parseClassPaths(Object sourceSet, JavaSourceSet.Builder result) {
            Collection<? extends File> compile = Collections.emptySet();
            boolean compileResolved = false;
            try {
                compile = sourceSetMethods.getCompileClasspath(sourceSet).getFiles();
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
                runtime = sourceSetMethods.getRuntimeClasspath(sourceSet).getFiles();
                if (!compileResolved) {
                    compile = runtime;
                }
            } catch (Throwable ex) {
                result.setRuntimeClassPathProblem(ex);
            }

            result.setClasspaths(new JavaClassPaths(compile, runtime));
        }

        private JavaSourceGroup parseSourceGroup(JavaSourceGroupName name, Object sourceGroup) throws Exception {
            Collection<? extends File> srcDirs = sourceDirectorySetMethods.getSrcDirs(sourceGroup);
            return new JavaSourceGroup(name, srcDirs);
        }

        private JavaOutputDirs parseOutputDirs(Object outputDirs) throws Exception {
            List<File> otherDirs = new LinkedList<File>();

            Iterable<? extends File> files =
                    sourceSetOutputMethods.getDirs(outputDirs).getFiles();

            for (File file: files) {
                otherDirs.add(file);
            }

            File classesDir = sourceSetOutputMethods.getClassesDir(outputDirs);
            File resourcesDir = sourceSetOutputMethods.getResourcesDir(outputDirs);

            return new JavaOutputDirs(classesDir, resourcesDir, otherDirs);
        }
    }
}
