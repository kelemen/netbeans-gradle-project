package org.netbeans.gradle.project.groovy;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.model.java.GroovyBaseModel;
import org.netbeans.gradle.model.java.JavaModelBuilders;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.api.entry.ModelLoadResult;
import org.netbeans.gradle.project.api.entry.ParsedModel;
import org.netbeans.gradle.project.api.modelquery.GradleModelDef;
import org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery2;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = GradleProjectExtensionDef.class, position = 990)
public final class GroovyExtensionDef implements GradleProjectExtensionDef<NbGroovyModel> {
    public static final String EXTENSION_NAME = "org.netbeans.gradle.project.groovy.GroovyExtensionDef";

    private final Lookup lookup;

    public GroovyExtensionDef() {
        this.lookup = Lookups.fixed(new ModelQuery());
    }

    @Override
    public String getName() {
        return EXTENSION_NAME;
    }

    @Override
    public String getDisplayName() {
        return "Groovy";
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public Class<NbGroovyModel> getModelType() {
        return NbGroovyModel.class;
    }

    @Override
    public ParsedModel<NbGroovyModel> parseModel(ModelLoadResult retrievedModels) {
        GroovyBaseModel baseModel = retrievedModels.getMainProjectModels().lookup(GroovyBaseModel.class);
        NbGroovyModel result = baseModel != null ? NbGroovyModel.DEFAULT : null;
        return new ParsedModel<>(result);
    }

    @Override
    public GradleProjectExtension2<NbGroovyModel> createExtension(Project project) throws IOException {
        return new GroovyExtension(project);
    }

    @Override
    public Set<String> getSuppressedExtensions() {
        return Collections.emptySet();
    }

    private static final class ModelQuery implements GradleModelDefQuery2 {
        @Override
        public GradleModelDef getModelDef(GradleTarget gradleTarget) {
            return GradleModelDef.fromProjectInfoBuilders2(JavaModelBuilders.GROOVY_BASE_BUILDER);
        }
    }
}
