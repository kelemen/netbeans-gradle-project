package org.netbeans.gradle.project.query;

import javax.swing.event.ChangeListener;

public interface NotificationRegistry {
    public void addChangeListener(ChangeListener listener);
    public void removeChangeListener(ChangeListener listener);
}
