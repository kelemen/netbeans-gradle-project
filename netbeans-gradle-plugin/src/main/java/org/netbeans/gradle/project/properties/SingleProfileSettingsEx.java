package org.netbeans.gradle.project.properties;

import java.util.Collection;
import org.netbeans.gradle.project.api.config.SingleProfileSettings;
import org.w3c.dom.Element;

public interface SingleProfileSettingsEx extends SingleProfileSettings {
    public Collection<DomElementKey> getAuxConfigKeys();
    public Element getAuxConfigValue(DomElementKey key);
    public boolean setAuxConfigValue(DomElementKey key, Element value);

    public void saveAndWait();
}
