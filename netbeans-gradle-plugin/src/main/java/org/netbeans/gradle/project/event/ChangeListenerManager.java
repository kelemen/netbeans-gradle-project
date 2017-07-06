package org.netbeans.gradle.project.event;

import org.jtrim2.event.ListenerRegistry;

public interface ChangeListenerManager extends ListenerRegistry<Runnable> {
    public void fireEventually();
}
