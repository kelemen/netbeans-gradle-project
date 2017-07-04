package org.netbeans.gradle.project.api.entry;

import java.io.IOException;
import java.util.Collections;
import java.util.Set;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;
import org.openide.util.lookup.ServiceProvider;

@ServiceProvider(service = GradleProjectExtensionDef.class, position = 500)
public class GradleTestExtensionQuery implements GradleProjectExtensionDef<Object> {
    @Override
    public String getName() {
        return getClass().getName();
    }

    @Override
    public String getDisplayName() {
        return "Test Extension: " + getClass().getSimpleName();
    }

    @Override
    public Lookup getLookup() {
        return Lookup.EMPTY;
    }

    @Override
    public Class<Object> getModelType() {
        return Object.class;
    }

    @Override
    public ParsedModel<Object> parseModel(ModelLoadResult retrievedModels) {
        return new ParsedModel<>(new Object());
    }

    @Override
    public GradleProjectExtension2<Object> createExtension(Project project) throws IOException {
        return new GradleTestExtension();
    }

    @Override
    public Set<String> getSuppressedExtensions() {
        return Collections.emptySet();
    }
}
