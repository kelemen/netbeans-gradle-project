package org.netbeans.gradle.project.properties;

public interface StringBasedProperty<ValueType> extends MutableProperty<ValueType> {
    public void setValueFromString(String strValue);
    public String getValueAsString();
}
