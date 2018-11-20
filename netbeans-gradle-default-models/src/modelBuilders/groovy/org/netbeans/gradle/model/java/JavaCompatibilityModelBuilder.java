package org.netbeans.gradle.model.java;

import org.gradle.api.Project;
import org.gradle.api.plugins.JavaPluginConvention;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.BuilderUtils;

/**
 * Defines a {@code ProjectInfoBuilder2} which is able to extract
 * {@link JavaCompatibilityModel} from a Gradle project.
 * <P>
 * Since this builder does not have any input argument, it is singleton and its
 * instance can be accessed through {@code JavaCompatibilityModelBuilder.INSTANCE}.
 */
enum JavaCompatibilityModelBuilder
implements
        ProjectInfoBuilder2<JavaCompatibilityModel> {

    /**
     * The one and only instance of {@code JavaCompatibilityModelBuilder}.
     */
    INSTANCE;

    /**
     * Extracts and returns the {@code JavaCompatibilityModel} from the given
     * project or returns {@code null} if the project does not uses the 'java'
     * plugin.
     *
     * @param project the project from which the information is to be extracted.
     *   This argument cannot be {@code null}.
     *
     * @return the {@code JavaCompatibilityModel} extracted from the given
     *   project or {@code null} if the project does not applies the "java"
     *   plugin
     */
    @Override
    public JavaCompatibilityModel getProjectInfo(Object project) {
        return getProjectInfo((Project)project);
    }

    private JavaCompatibilityModel getProjectInfo(Project project) {
        JavaPluginConvention javaPlugin = project.getConvention().findPlugin(JavaPluginConvention.class);
        if (javaPlugin == null) {
            return null;
        }

        String srcLevel = javaPlugin.getSourceCompatibility().toString();
        String targetLevel = javaPlugin.getTargetCompatibility().toString();

        return new JavaCompatibilityModel(srcLevel, targetLevel);
    }

    /** {@inheritDoc } */
    @Override
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }
}
