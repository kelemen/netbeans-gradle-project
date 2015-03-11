package org.netbeans.gradle.project.properties2;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.ListenerRef;

public interface ActiveSettingsQuery {
    public <ValueType> AcquiredPropertySource<ValueType> acquireProperty(PropertyDef<?, ValueType> propertyDef);

    public void waitForLoadedOnce(CancellationToken cancelToken);
    public ListenerRef notifyWhenLoadedOnce(Runnable listener);
}
