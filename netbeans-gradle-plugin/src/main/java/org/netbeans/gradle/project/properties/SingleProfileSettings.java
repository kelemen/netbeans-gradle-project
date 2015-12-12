package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.ProfileKey;
import javax.annotation.Nullable;
import org.jtrim.property.MutableProperty;

public interface SingleProfileSettings {
    @Nullable
    public ProfileKey getKey();

    // FIXME: ValueKey should be ?
    public <ValueKey, ValueType> MutableProperty<ValueType> getProperty(PropertyDef<ValueKey, ValueType> propertyDef);
}
