package org.netbeans.gradle.project.properties;

import org.jtrim.property.MutableProperty;

public interface StringBasedProperty<ValueType> extends MutableProperty<ValueType> {
    public void setValueFromString(String strValue);
    public String getValueAsString();
}
