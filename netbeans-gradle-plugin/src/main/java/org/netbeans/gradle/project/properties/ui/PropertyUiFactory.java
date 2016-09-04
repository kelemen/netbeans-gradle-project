package org.netbeans.gradle.project.properties.ui;

import javax.swing.JComponent;

public final class PropertyUiFactory {
    public static <V> JComponent create(
            SinglePropertyUi<V> ui
    ) {
        return null;
    }

    private PropertyUiFactory() {
        throw new AssertionError();
    }
}
