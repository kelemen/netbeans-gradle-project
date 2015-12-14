package org.netbeans.gradle.project.api.config;

import javax.annotation.Nullable;
import org.jtrim.property.MutableProperty;

public interface SingleProfileSettings {
    @Nullable
    public ProfileKey getKey();

    public <ValueType> MutableProperty<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef);
}
