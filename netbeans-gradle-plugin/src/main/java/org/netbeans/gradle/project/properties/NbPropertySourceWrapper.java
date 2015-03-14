package org.netbeans.gradle.project.properties;

import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.api.property.NbPropertySource;

public final class NbPropertySourceWrapper<ValueType>
implements
        NbPropertySource<ValueType> {
    private final PropertySource<ValueType> source;

    public NbPropertySourceWrapper(PropertySource<ValueType> source) {
        ExceptionHelper.checkNotNullArgument(source, "source");
        this.source = source;
    }

    @Override
    public ValueType getValue() {
        return source.getValue();
    }

    @Override
    public NbListenerRef addChangeListener(Runnable listener) {
        return NbListenerRefs.asNbRef(source.addChangeListener(listener));
    }
}
