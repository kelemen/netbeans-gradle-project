package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Element;

public final class AuxConfigSource {
    private final DomElementKey key;
    private final PropertySource<Element> source;

    public AuxConfigSource(String elementName, String namespace, PropertySource<Element> source) {
        this(new DomElementKey(elementName, namespace), source);
    }

    public AuxConfigSource(DomElementKey key, PropertySource<Element> source) {
        ExceptionHelper.checkNotNullArgument(key, "key");
        ExceptionHelper.checkNotNullArgument(source, "source");

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
