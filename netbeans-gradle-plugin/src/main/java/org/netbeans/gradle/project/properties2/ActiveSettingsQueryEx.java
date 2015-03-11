package org.netbeans.gradle.project.properties2;

import org.netbeans.gradle.project.properties.DomElementKey;
import org.w3c.dom.Element;

public interface ActiveSettingsQueryEx extends ActiveSettingsQuery {
    public Element getAuxConfigValue(DomElementKey key);
}
