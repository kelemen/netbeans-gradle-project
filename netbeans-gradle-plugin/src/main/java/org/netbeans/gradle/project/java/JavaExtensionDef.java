package org.netbeans.gradle.project.java;

import java.io.File;
import java.io.IOException;
import java.util.Collection;
import java.util.Collections;
import java.util.Map;
import java.util.Set;
import org.gradle.tooling.model.idea.IdeaProject;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.JavaModelBuilders;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.api.entry.ModelLoadResult;
import org.netbeans.gradle.project.api.entry.ParsedModel;
import org.netbeans.gradle.project.api.modelquery.GradleModelDef;
import org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery1;
import org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery2;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.java.model.JavaModelSource;
import org.netbeans.gradle.project.java.model.JavaParsingUtils;
import org.netbeans.gradle.project.java.model.NbJavaModel;
import org.netbeans.gradle.project.java.model.NbJavaModule;
import org.netbeans.gradle.project.others.OtherPlugins;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = GradleProjectExtensionDef.class, position = 1000)
public final class JavaExtensionDef implements GradleProjectExtensionDef<NbJavaModel> {
    // Do not return JavaExtension.class.getName() because this string must
    // remain the same even if this class is renamed.
    public static final String EXTENSION_NAME = "org.netbeans.gradle.project.java.JavaExtension";
    private final Lookup lookup;

    public JavaExtensionDef() {
        this.lookup = Lookups.fixed(new Query1(), new Query2());
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @Override
    public String getDisplayName() {
        return "J2SE";
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public Class<NbJavaModel> getModelType() {
        return NbJavaModel.class;
    }

    private static NbJavaModel createReliableModel(GradleTarget evaluationEnvironment, NbJavaModule mainModule) {
        return NbJavaModel.createModel(
                evaluationEnvironment,
                JavaModelSource.GRADLE_1_8_API,
                mainModule);
    }

    private Map<File, NbJavaModel> parseFromNewModels(ModelLoadResult retrievedModels) {
        GradleTarget evaluationEnvironment = retrievedModels.getEvaluationEnvironment();
        Collection<NbJavaModule> modules = JavaParsingUtils.parseModules(retrievedModels);

        Map<File, NbJavaModel> result = CollectionUtils.newHashMap(modules.size());
        for (NbJavaModule module: modules) {
            NbJavaModel model = createReliableModel(evaluationEnvironment, module);
            result.put(module.getModuleDir(), model);
        }

        for (File projectDir: retrievedModels.getEvaluatedProjectsModel().keySet()) {
            if (!result.containsKey(projectDir)) {
                result.put(projectDir, null);
            }
        }

        return result;
    }

    private ParsedModel<NbJavaModel> parseModelImpl(ModelLoadResult retrievedModels) throws IOException {
        Map<File, NbJavaModel> result = parseFromNewModels(retrievedModels);

        NbJavaModel mainModule = result.get(retrievedModels.getMainProjectDir());
        return new ParsedModel<>(mainModule, result);
    }

    @Override
    public ParsedModel<NbJavaModel> parseModel(ModelLoadResult retrievedModels) {
        try {
            return parseModelImpl(retrievedModels);
        } catch (IOException ex) {
            throw new RuntimeException(ex);
        }
    }

    @Override
    public GradleProjectExtension2<NbJavaModel> createExtension(Project project) throws IOException {
        return JavaExtension.create(project);
    }

    @Override
    public Set<String> getSuppressedExtensions() {
        return Collections.emptySet();
    }

    private static final class Query1 implements GradleModelDefQuery1 {
        private static final Collection<Class<?>> RESULT = Collections.<Class<?>>singleton(IdeaProject.class);

        @Override
        public Collection<Class<?>> getToolingModels(GradleTarget gradleTarget) {
            return RESULT;
        }
    }

    private static final class Query2 implements GradleModelDefQuery2 {
        private static final GradleModelDef RESULT = GradleModelDef.fromProjectInfoBuilders2(
                JavaModelBuilders.JAR_OUTPUTS_BUILDER,
                JavaModelBuilders.JAVA_SOURCES_BUILDER_COMPLETE,
                JavaModelBuilders.JAVA_COMPATIBILITY_BUILDER,
                JavaModelBuilders.JAVA_TEST_BUILDER,
                JavaModelBuilders.JACOCO_BUILDER,
                JavaModelBuilders.WAR_FOLDERS_BUILDER);

        private static final GradleModelDef RESULT_WITHOUT_WAR = GradleModelDef.fromProjectInfoBuilders2(
                JavaModelBuilders.JAR_OUTPUTS_BUILDER,
                JavaModelBuilders.JAVA_SOURCES_BUILDER_COMPLETE,
                JavaModelBuilders.JAVA_COMPATIBILITY_BUILDER,
                JavaModelBuilders.JAVA_TEST_BUILDER,
                JavaModelBuilders.JACOCO_BUILDER);

        @Override
        public GradleModelDef getModelDef(GradleTarget gradleTarget) {
            return OtherPlugins.hasJavaEEExtension()
                    ? RESULT_WITHOUT_WAR
                    : RESULT;
        }
    }
}
