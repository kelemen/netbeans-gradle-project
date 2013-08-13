package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;

public interface PropertySource<ValueType> {
    public ValueType getValue();
    public boolean isDefault();

    public void addChangeListener(ChangeListener listener);
    public void removeChangeListener(ChangeListener listener);
}
