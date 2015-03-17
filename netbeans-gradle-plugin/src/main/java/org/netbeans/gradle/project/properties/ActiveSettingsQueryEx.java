package org.netbeans.gradle.project.properties;

import org.w3c.dom.Element;

public interface ActiveSettingsQueryEx extends ActiveSettingsQuery {
    public Element getAuxConfigValue(DomElementKey key);
}
