package org.netbeans.gradle.project.properties2;

import org.jtrim.cancel.CancellationToken;
import org.netbeans.gradle.project.properties.DomElementKey;
import org.w3c.dom.Element;

public interface ActiveSettingsQuery {
    public Element getAuxConfigValue(DomElementKey key);
    public <ValueType> AcquiredPropertySource<ValueType> acquireProperty(PropertyDef<?, ValueType> propertyDef);

    public void waitForLoadedOnce(CancellationToken cancelToken);
    public void notifyWhenLoadedOnce(Runnable listener);
}
