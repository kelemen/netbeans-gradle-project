package org.netbeans.gradle.project.properties2;

import org.jtrim.property.PropertySource;

public interface PropertyValueDef<ValueKey, ValueType> {
    public PropertySource<ValueType> property(ValueKey valueKey);
    public ValueKey getKeyFromValue(ValueType value);
}
