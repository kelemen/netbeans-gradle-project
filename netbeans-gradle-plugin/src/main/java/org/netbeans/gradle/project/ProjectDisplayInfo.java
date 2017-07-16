package org.netbeans.gradle.project;

import java.util.Objects;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.model.NbGradleModel;

public final class ProjectDisplayInfo {
    private final PropertySource<String> displayName;
    private final PropertySource<String> description;

    public ProjectDisplayInfo(
            PropertySource<? extends NbGradleModel> currentModel,
            PropertySource<? extends String> displayNamePattern) {
        Objects.requireNonNull(currentModel, "currentModel");
        Objects.requireNonNull(displayNamePattern, "displayNamePattern");

        this.displayName = PropertyFactory.combine(currentModel, displayNamePattern, NbGradleModel::getDisplayName);
        this.description = PropertyFactory.convert(currentModel, NbGradleModel::getDescription);
    }

    public PropertySource<String> displayName() {
        return displayName;
    }

    public PropertySource<String> description() {
        return description;
    }
}
