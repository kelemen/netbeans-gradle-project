package org.netbeans.gradle.project.api.entry;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.gradle.tooling.model.eclipse.EclipseProject;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.api.modelquery.GradleModelDefQuery1;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class SingleModelExtensionQuery implements GradleProjectExtensionDef<EclipseProject> {
    private final Lookup lookup;

    public SingleModelExtensionQuery() {
        GradleModelDefQuery1 modelQuery = gradleTarget -> Collections.singleton(EclipseProject.class);

        this.lookup = Lookups.fixed(modelQuery);
    }

    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public String getDisplayName() {
        return "TestExtension: " + getClass().getSimpleName();
    }

    @Override
    public Lookup getLookup() {
        return lookup;
    }

    @Override
    public Class<EclipseProject> getModelType() {
        return EclipseProject.class;
    }

    @Override
    public ParsedModel<EclipseProject> parseModel(ModelLoadResult retrievedModels) {
        EclipseProject model = retrievedModels.getMainProjectModels().lookup(EclipseProject.class);
        return new ParsedModel<>(model);
    }

    @Override
    public GradleProjectExtension2<EclipseProject> createExtension(Project project) throws IOException {
        return new SingleModelExtension<>();
    }

    @Override
    public Set<String> getSuppressedExtensions() {
        return Collections.emptySet();
    }
}
