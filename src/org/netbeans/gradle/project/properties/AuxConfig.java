package org.netbeans.gradle.project.properties;

import org.w3c.dom.Element;

public final class AuxConfig {
    private final DomElementKey key;
    private final Element value;

    public AuxConfig(String elementName, String namespace, Element value) {
        this(new DomElementKey(elementName, namespace), value);
    }

    public AuxConfig(DomElementKey key, Element value) {
        if (key == null) throw new NullPointerException("key");
        this.key = key;
        this.value = value != null ? (Element)value.cloneNode(true) : null;
    }

    public DomElementKey getKey() {
        return key;
    }

    public Element getValue() {
        return (Element)value.cloneNode(true);
    }
}
