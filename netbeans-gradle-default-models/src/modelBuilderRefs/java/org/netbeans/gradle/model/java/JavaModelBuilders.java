package org.netbeans.gradle.model.java;

import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.internal.EnumProjectInfoBuilderRef;

public class JavaModelBuilders {
    public static final ProjectInfoBuilder<JacocoModel> JACOCO_BUILDER
            = new EnumProjectInfoBuilderRef<JacocoModel>(JacocoModel.class, "JacocoModelBuilder");

    public static final ProjectInfoBuilder<JarOutputsModel> JAR_OUTPUTS_BUILDER
            = new EnumProjectInfoBuilderRef<JarOutputsModel>(JarOutputsModel.class, "JarOutputsModelBuilder");

    public static final ProjectInfoBuilder<JavaCompatibilityModel> JAVA_COMPATIBILITY_BUILDER
            = new EnumProjectInfoBuilderRef<JavaCompatibilityModel>(JavaCompatibilityModel.class, "JavaCompatibilityModelBuilder");

    public static final ProjectInfoBuilder<JavaSourcesModel> JAVA_SOURCES_BUILDER_COMPLETE
            = new EnumProjectInfoBuilderRef<JavaSourcesModel>(JavaSourcesModel.class, "JavaSourcesModelBuilder", "COMPLETE");

    public static final ProjectInfoBuilder<JavaSourcesModel> JAVA_SOURCES_BUILDER_ONLY_COMPILE
            = new EnumProjectInfoBuilderRef<JavaSourcesModel>(JavaSourcesModel.class, "JavaSourcesModelBuilder", "ONLY_COMPILE");

    public static final ProjectInfoBuilder<JavaTestModel> JAVA_TEST_BUILDER
            = new EnumProjectInfoBuilderRef<JavaTestModel>(JavaTestModel.class, "JavaTestModelBuilder");

    public static final ProjectInfoBuilder<WarFoldersModel> WAR_FOLDERS_BUILDER
            = new EnumProjectInfoBuilderRef<WarFoldersModel>(WarFoldersModel.class, "WarFoldersModelBuilder");

    private JavaModelBuilders() {
        throw new AssertionError();
    }
}
