package org.netbeans.gradle.project.properties;

import org.jtrim2.event.ListenerRef;

public interface LoadableSingleProfileSettingsEx extends SingleProfileSettingsEx {
    public void ensureLoadedAndWait();
    public void ensureLoaded();
    public ListenerRef notifyWhenLoaded(Runnable runnable);
}
