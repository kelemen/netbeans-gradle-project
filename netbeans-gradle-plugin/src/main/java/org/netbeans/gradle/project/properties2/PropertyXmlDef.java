package org.netbeans.gradle.project.properties2;

import org.w3c.dom.Element;

public interface PropertyXmlDef<ValueKey> {
    public ValueKey loadFromXml(Element node);
    public void addToXml(Element parent, ValueKey value);
}
