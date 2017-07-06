package org.netbeans.gradle.project.properties;

import java.util.Objects;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.property.NbPropertySource;

public final class NbPropertySourceWrapper<ValueType>
implements
        NbPropertySource<ValueType> {
    private final PropertySource<ValueType> source;

    public NbPropertySourceWrapper(PropertySource<ValueType> source) {
        this.source = Objects.requireNonNull(source, "source");
    }

    @Override
    public ValueType getValue() {
        return source.getValue();
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        return source.addChangeListener(listener);
    }
}
