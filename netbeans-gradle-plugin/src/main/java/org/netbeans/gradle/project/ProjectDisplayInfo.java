package org.netbeans.gradle.project;

import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.util.NbBiFunction;

public final class ProjectDisplayInfo {
    private final PropertySource<String> displayName;
    private final PropertySource<String> description;

    public ProjectDisplayInfo(
            PropertySource<? extends NbGradleModel> currentModel,
            PropertySource<? extends String> displayNamePattern) {
        ExceptionHelper.checkNotNullArgument(currentModel, "currentModel");
        ExceptionHelper.checkNotNullArgument(displayNamePattern, "displayNamePattern");

        this.displayName = NbProperties.combine(currentModel, displayNamePattern, new NbBiFunction<NbGradleModel, String, String>() {
            @Override
            public String apply(NbGradleModel model, String pattern) {
                return model.getDisplayName(pattern);
            }
        });
        this.description = PropertyFactory.convert(currentModel, new ValueConverter<NbGradleModel, String>() {
            @Override
            public String convert(NbGradleModel input) {
                return input.getDescription();
            }
        });
    }

    public PropertySource<String> displayName() {
        return displayName;
    }

    public PropertySource<String> description() {
        return description;
    }
}
