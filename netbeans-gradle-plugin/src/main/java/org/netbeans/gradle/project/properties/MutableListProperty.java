package org.netbeans.gradle.project.properties;

import java.util.List;
import org.jtrim.event.ListenerRef;
import org.netbeans.gradle.model.util.CollectionUtils;

public final class MutableListProperty<ElementType> implements MutableProperty<List<ElementType>> {
    private final MutableProperty<List<ElementType>> wrapped;

    public MutableListProperty(List<? extends ElementType> value, boolean defaultValue) {
        this.wrapped = new DefaultMutableProperty<>(
                CollectionUtils.copyNullSafeList(value), defaultValue, false);
    }

    @Override
    public void setValueFromSource(PropertySource<? extends List<ElementType>> source) {
        wrapped.setValueFromSource(source);
    }

    @Override
    public void setValue(List<ElementType> value) {
        wrapped.setValue(CollectionUtils.copyNullSafeList(value));
    }

    @Override
    public List<ElementType> getValue() {
        return wrapped.getValue();
    }

    @Override
    public boolean isDefault() {
        return wrapped.isDefault();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return wrapped.addChangeListener(listener);
    }
}
