package org.netbeans.gradle.project.event;

import org.jtrim.event.ListenerRegistry;

public interface ChangeListenerManager extends ListenerRegistry<Runnable> {
    public interface PauseRef {
        public void unpause();
    }

    public PauseRef pauseManager();
    public void fireEventually();
}
