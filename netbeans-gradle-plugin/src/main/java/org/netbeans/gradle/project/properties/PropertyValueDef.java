package org.netbeans.gradle.project.properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.property.PropertySource;

public interface PropertyValueDef<ValueKey, ValueType> {
    @Nonnull
    public PropertySource<ValueType> property(@Nullable ValueKey valueKey);

    @Nullable
    public ValueKey getKeyFromValue(ValueType value);
}
