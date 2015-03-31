package org.netbeans.gradle.project.properties;

import org.jtrim.property.PropertySource;

public interface ActiveSettingsQuery {
    public <ValueType> PropertySource<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef);

    public PropertySource<ProjectProfileSettings> currentProfileSettings();
}
