package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Element;

public final class AuxConfigProperty {
    private final DomElementKey key;
    private final OldMutableProperty<Element> property;

    public AuxConfigProperty(String elementName, String namespace, OldMutableProperty<Element> property) {
        this(new DomElementKey(elementName, namespace), property);
    }

    public AuxConfigProperty(DomElementKey key, OldMutableProperty<Element> property) {
        ExceptionHelper.checkNotNullArgument(key, "key");
        ExceptionHelper.checkNotNullArgument(property, "property");

        this.key = key;
        this.property = property;
    }

    public DomElementKey getKey() {
        return key;
    }

    public OldMutableProperty<Element> getProperty() {
        return property;
    }
}
