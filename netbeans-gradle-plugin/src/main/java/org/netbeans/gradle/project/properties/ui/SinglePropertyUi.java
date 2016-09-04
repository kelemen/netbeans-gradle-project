package org.netbeans.gradle.project.properties.ui;

import javax.swing.JComponent;

public interface SinglePropertyUi<V> {
    public JComponent getComponent();

    public void setDisplayedValue(V value);
    public V getDisplayedValue();

    public void setEnabled(boolean enabled);
}
