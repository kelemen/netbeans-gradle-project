package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
import org.w3c.dom.Element;

public final class DomElementSource implements PropertySource<Element> {
    private final Element element;
    private final boolean defaultValue;

    public DomElementSource(Element element, boolean defaultValue) {
        this.element = element != null ? (Element)element.cloneNode(true) : null;
        this.defaultValue = defaultValue;
    }

    @Override
    public Element getValue() {
        return element != null ? (Element)element.cloneNode(true) : null;
    }

    @Override
    public boolean isDefault() {
        return defaultValue;
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return UnregisteredListenerRef.INSTANCE;
    }
}
