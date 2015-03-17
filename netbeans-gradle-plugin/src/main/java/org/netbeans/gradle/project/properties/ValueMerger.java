package org.netbeans.gradle.project.properties;

import javax.annotation.Nonnull;

public interface ValueMerger<ValueType> {
    public ValueType mergeValues(ValueType child, @Nonnull ValueReference<ValueType> parent);
}
