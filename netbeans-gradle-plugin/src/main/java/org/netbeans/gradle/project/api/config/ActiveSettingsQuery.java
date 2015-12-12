package org.netbeans.gradle.project.api.config;

import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.properties.SingleProfileSettings;

public interface ActiveSettingsQuery {
    public <ValueType> PropertySource<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef);

    public PropertySource<SingleProfileSettings> currentProfileSettings();
}
