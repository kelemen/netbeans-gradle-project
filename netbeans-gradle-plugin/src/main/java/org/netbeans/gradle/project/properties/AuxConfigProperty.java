package org.netbeans.gradle.project.properties;

import org.w3c.dom.Element;

public final class AuxConfigProperty {
    private final DomElementKey key;
    private final MutableProperty<Element> property;

    public AuxConfigProperty(String elementName, String namespace, MutableProperty<Element> property) {
        this(new DomElementKey(elementName, namespace), property);
    }

    public AuxConfigProperty(DomElementKey key, MutableProperty<Element> property) {
        if (key == null) throw new NullPointerException("key");
        if (property == null) throw new NullPointerException("property");

        this.key = key;
        this.property = property;
    }

    public DomElementKey getKey() {
        return key;
    }

    public MutableProperty<Element> getProperty() {
        return property;
    }
}
