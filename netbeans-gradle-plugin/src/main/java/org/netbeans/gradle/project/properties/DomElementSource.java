package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;
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
    public void addChangeListener(ChangeListener listener) {
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
    }
}
