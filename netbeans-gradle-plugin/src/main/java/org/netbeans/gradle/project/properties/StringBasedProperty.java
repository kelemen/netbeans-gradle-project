package org.netbeans.gradle.project.properties;

public interface StringBasedProperty<ValueType> extends OldMutableProperty<ValueType> {
    public void setValueFromString(String strValue);
    public String getValueAsString();
}
