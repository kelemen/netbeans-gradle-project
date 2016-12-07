package org.netbeans.gradle.project.java.model;

import java.io.File;
import java.util.List;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.NbFunction;

public final class ProjectDependencyCandidate {
    private static final PropertySource<JavaProjectDependencyDef> NO_DEPENDENCY
            = PropertyFactory.<JavaProjectDependencyDef>constSource(null);

    private final Project project;
    private final File dependency;

    private final PropertySource<JavaProjectDependencyDef> projectDependency;

    public ProjectDependencyCandidate(Project project, File dependency) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(dependency, "dependency");

        this.project = project;
        this.dependency = dependency;
        this.projectDependency = javaModelOfProject(dependency, JavaExtension.extensionOfProject(project));
    }

    private static PropertySource<JavaProjectDependencyDef> javaModelOfProject(
            final File dependency,
            PropertySource<JavaExtension> extRef) {

        return NbProperties.propertyOfProperty(extRef, new NbFunction<JavaExtension, PropertySource<JavaProjectDependencyDef>>() {
            @Override
            public PropertySource<JavaProjectDependencyDef> apply(JavaExtension ext) {
                return ext != null
                        ? asProjectDepedencyDef(dependency, ext, ext.currentModel())
                        : NO_DEPENDENCY;
            }
        });
    }

    private static PropertySource<JavaProjectDependencyDef> asProjectDepedencyDef(
            final File dependency,
            final JavaExtension ext,
            final PropertySource<NbJavaModel> modelRef) {
        return PropertyFactory.convert(modelRef, new ValueConverter<NbJavaModel, JavaProjectDependencyDef>() {
            @Override
            public JavaProjectDependencyDef convert(NbJavaModel input) {
                if (input == null) {
                    return null;
                }

                NbJavaModule module = input.getMainModule();
                List<JavaSourceSet> sourceSets = module.getSourceSetsForOutput(dependency);
                if (sourceSets.isEmpty()) {
                    return null;
                }

                return new JavaProjectDependencyDef(ext, sourceSets);
            }
        });
    }

    public Project getProject() {
        return project;
    }

    public PropertySource<JavaProjectDependencyDef> projectDependency() {
        return projectDependency;
    }

    public File getDependency() {
        return dependency;
    }

    @Override
    public String toString() {
        return "ProjectDependencyCandidate{" + dependency + '}';
    }
}
