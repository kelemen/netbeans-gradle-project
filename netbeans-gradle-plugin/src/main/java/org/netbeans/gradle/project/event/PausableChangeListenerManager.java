package org.netbeans.gradle.project.event;

public interface PausableChangeListenerManager extends ChangeListenerManager {
    public interface PauseRef {
        public void unpause();
    }

    public PauseRef pauseManager();
}
