package org.netbeans.gradle.project.properties;

import org.w3c.dom.Element;

public final class AuxConfigSource {
    private final DomElementKey key;
    private final PropertySource<Element> source;

    public AuxConfigSource(String elementName, String namespace, PropertySource<Element> source) {
        this(new DomElementKey(elementName, namespace), source);
    }

    public AuxConfigSource(DomElementKey key, PropertySource<Element> source) {
        if (key == null) throw new NullPointerException("key");
        if (source == null) throw new NullPointerException("source");

        this.key = key;
        this.source = source;
    }

    public DomElementKey getKey() {
        return key;
    }

    public PropertySource<Element> getSource() {
        return source;
    }
}
