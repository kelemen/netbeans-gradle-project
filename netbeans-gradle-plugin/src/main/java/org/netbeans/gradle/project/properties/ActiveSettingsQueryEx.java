package org.netbeans.gradle.project.properties;

import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.w3c.dom.Element;

public interface ActiveSettingsQueryEx extends ActiveSettingsQuery {
    public Element getAuxConfigValue(DomElementKey key);
    public PropertySource<SingleProfileSettingsEx> currentProfileSettingsEx();
}
