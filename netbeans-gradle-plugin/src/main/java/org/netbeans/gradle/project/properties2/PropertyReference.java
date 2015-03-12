package org.netbeans.gradle.project.properties2;

import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;

public final class PropertyReference<ValueType> {
    private final PropertyDef<?, ValueType> propertyDef;
    private final ActiveSettingsQuery activeSettingsQuery;
    private final PropertySource<ValueType> activeSource;

    public PropertyReference(PropertyDef<?, ValueType> propertyDef, ActiveSettingsQuery activeSettingsQuery) {
        ExceptionHelper.checkNotNullArgument(propertyDef, "propertyDef");
        ExceptionHelper.checkNotNullArgument(activeSettingsQuery, "activeSettingsQuery");

        this.propertyDef = propertyDef;
        this.activeSettingsQuery = activeSettingsQuery;
        this.activeSource = activeSettingsQuery.getProperty(propertyDef);
    }

    public PropertySource<ValueType> getActiveSource() {
        return activeSource;
    }

    public ValueType getActiveValue() {
        return activeSource.getValue();
    }

    public PropertyDef<?, ValueType> getPropertyDef() {
        return propertyDef;
    }

    public MutableProperty<ValueType> tryGetForActiveProfile() {
        ProjectProfileSettings settings = activeSettingsQuery.tryGetCurrentProfileSettings();
        return settings != null ? forProfile(settings) : null;
    }

    public MutableProperty<ValueType> forProfile(ProjectProfileSettings settings) {
        return settings.getProperty(propertyDef);
    }
}
