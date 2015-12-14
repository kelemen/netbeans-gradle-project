package org.netbeans.gradle.project.properties;

import javax.annotation.Nullable;
import org.jtrim.property.MutableProperty;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.gradle.project.api.config.PropertyDef;

public interface SingleProfileSettings {
    @Nullable
    public ProfileKey getKey();

    public <ValueType> MutableProperty<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef);
}
