package org.netbeans.gradle.project.properties2;

public interface ValueMerger<ValueType> {
    public ValueType mergeValues(ValueType child, ValueReference<ValueType> parent);
}
