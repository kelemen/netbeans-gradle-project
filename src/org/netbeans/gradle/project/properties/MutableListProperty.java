package org.netbeans.gradle.project.properties;

import java.util.List;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.CollectionUtils;
import org.openide.util.ChangeSupport;

public final class MutableListProperty<ElementType> implements MutableProperty<List<ElementType>> {
    private List<ElementType> value;
    private final ChangeSupport changes;

    public MutableListProperty(List<? extends ElementType> value) {
        this.value = CollectionUtils.copyNullSafeList(value);
        this.changes = new ChangeSupport(this);
    }

    @Override
    public void setValue(List<ElementType> value) {
        this.value = CollectionUtils.copyNullSafeList(value);
        changes.fireChange();
    }

    @Override
    public List<ElementType> getValue() {
        return value;
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }
}
