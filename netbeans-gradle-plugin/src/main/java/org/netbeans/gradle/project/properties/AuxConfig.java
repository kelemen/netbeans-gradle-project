package org.netbeans.gradle.project.properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Element;

public final class AuxConfig {
    private final DomElementKey key;
    private final Element value;

    public AuxConfig(String elementName, String namespace, Element value) {
        this(new DomElementKey(elementName, namespace), value);
    }

    public AuxConfig(@Nonnull DomElementKey key, @Nullable Element value) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        this.key = key;
        this.value = value != null ? (Element)value.cloneNode(true) : null;
    }

    @Nonnull
    public DomElementKey getKey() {
        return key;
    }

    @Nonnull
    public Element getValue() {
        return (Element)value.cloneNode(true);
    }
}
