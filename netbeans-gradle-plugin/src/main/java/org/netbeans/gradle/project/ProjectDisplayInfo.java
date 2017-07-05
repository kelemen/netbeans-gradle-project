package org.netbeans.gradle.project;

import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.properties.NbProperties;

public final class ProjectDisplayInfo {
    private final PropertySource<String> displayName;
    private final PropertySource<String> description;

    public ProjectDisplayInfo(
            PropertySource<? extends NbGradleModel> currentModel,
            PropertySource<? extends String> displayNamePattern) {
        ExceptionHelper.checkNotNullArgument(currentModel, "currentModel");
        ExceptionHelper.checkNotNullArgument(displayNamePattern, "displayNamePattern");

        this.displayName = NbProperties.combine(currentModel, displayNamePattern, NbGradleModel::getDisplayName);
        this.description = PropertyFactory.convert(currentModel, NbGradleModel::getDescription);
    }

    public PropertySource<String> displayName() {
        return displayName;
    }

    public PropertySource<String> description() {
        return description;
    }
}
