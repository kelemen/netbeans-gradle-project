package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeListener;
import org.w3c.dom.Element;

public final class DomElementProperty implements MutableProperty<Element> {
    private final MutableProperty<Element> wrapped;

    public DomElementProperty() {
        this.wrapped = new DefaultMutableProperty<Element>(new DomElementSource(null, true), true);
    }

    @Override
    public void setValueFromSource(PropertySource<? extends Element> source) {
        wrapped.setValueFromSource(source);
    }

    @Override
    public void setValue(Element value) {
        wrapped.setValueFromSource(new DomElementSource(value, false));
    }

    @Override
    public Element getValue() {
        return wrapped.getValue();
    }

    @Override
    public boolean isDefault() {
        return wrapped.isDefault();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        wrapped.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        wrapped.removeChangeListener(listener);
    }
}
