package org.netbeans.gradle.project.properties;

import org.jtrim.property.MutableProperty;

public interface SingleProfileSettings {
    public ProfileSettingsKey getKey();

    public <ValueKey, ValueType> MutableProperty<ValueType> getProperty(PropertyDef<ValueKey, ValueType> propertyDef);
}
