package org.netbeans.gradle.model.java;

import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.api.util.ModelBuilders;

public class JavaModelBuilders {
    public static final ProjectInfoBuilder2<JacocoModel> JACOCO_BUILDER
            = ModelBuilders.wrapEnumBuilder(JacocoModel.class, "JacocoModelBuilder");

    public static final ProjectInfoBuilder2<JarOutputsModel> JAR_OUTPUTS_BUILDER
            = ModelBuilders.wrapEnumBuilder(JarOutputsModel.class, "JarOutputsModelBuilder");

    public static final ProjectInfoBuilder2<JavaCompatibilityModel> JAVA_COMPATIBILITY_BUILDER
            = ModelBuilders.wrapEnumBuilder(JavaCompatibilityModel.class, "JavaCompatibilityModelBuilder");

    public static final ProjectInfoBuilder2<JavaSourcesModel> JAVA_SOURCES_BUILDER_COMPLETE
            = ModelBuilders.wrapEnumBuilder(JavaSourcesModel.class, "JavaSourcesModelBuilder", "COMPLETE");

    public static final ProjectInfoBuilder2<JavaSourcesModel> JAVA_SOURCES_BUILDER_ONLY_COMPILE
            = ModelBuilders.wrapEnumBuilder(JavaSourcesModel.class, "JavaSourcesModelBuilder", "ONLY_COMPILE");

    public static final ProjectInfoBuilder2<JavaTestModel> JAVA_TEST_BUILDER
            = ModelBuilders.wrapEnumBuilder(JavaTestModel.class, "JavaTestModelBuilder");

    public static final ProjectInfoBuilder2<WarFoldersModel> WAR_FOLDERS_BUILDER
            = ModelBuilders.wrapEnumBuilder(WarFoldersModel.class, "WarFoldersModelBuilder");

    public static final ProjectInfoBuilder2<GroovyBaseModel> GROOVY_BASE_BUILDER
            = ModelBuilders.wrapEnumBuilder(GroovyBaseModel.class, "GroovyBaseModelBuilder");

    private JavaModelBuilders() {
        throw new AssertionError();
    }
}
