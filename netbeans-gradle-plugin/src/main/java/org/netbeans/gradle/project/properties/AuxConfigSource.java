package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Element;

public final class AuxConfigSource {
    private final DomElementKey key;
    private final OldPropertySource<Element> source;

    public AuxConfigSource(String elementName, String namespace, OldPropertySource<Element> source) {
        this(new DomElementKey(elementName, namespace), source);
    }

    public AuxConfigSource(DomElementKey key, OldPropertySource<Element> source) {
        ExceptionHelper.checkNotNullArgument(key, "key");
        ExceptionHelper.checkNotNullArgument(source, "source");

        this.key = key;
        this.source = source;
    }

    public DomElementKey getKey() {
        return key;
    }

    public OldPropertySource<Element> getSource() {
        return source;
    }
}
