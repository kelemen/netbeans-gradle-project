package org.netbeans.gradle.project.properties2;

import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertySource;

public interface ActiveSettingsQuery {
    public <ValueType> PropertySource<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef);

    public PropertySource<ProjectProfileSettings> currentProfileSettings();

    public void waitForLoadedOnce(CancellationToken cancelToken);
    public ListenerRef notifyWhenLoadedOnce(Runnable listener);
}
