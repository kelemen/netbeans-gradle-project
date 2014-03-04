package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.event.NbListenerRef;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.api.property.NbPropertySource;

public final class NbPropertySourceWrapper<ValueType>
implements
        NbPropertySource<ValueType> {
    private final OldPropertySource<ValueType> source;

    public NbPropertySourceWrapper(OldPropertySource<ValueType> source) {
        ExceptionHelper.checkNotNullArgument(source, "source");
        this.source = source;
    }

    public NbPropertySourceWrapper(OldMutableProperty<ValueType> property) {
        this(new PropertySourceWrapper<>(property));
    }

    @Override
    public ValueType getValue() {
        return source.getValue();
    }

    @Override
    public NbListenerRef addChangeListener(Runnable listener) {
        return NbListenerRefs.asNbRef(source.addChangeListener(listener));
    }

    private static class PropertySourceWrapper<ValueType> implements OldPropertySource<ValueType> {
        private final OldMutableProperty<ValueType> property;

        public PropertySourceWrapper(OldMutableProperty<ValueType> property) {
            ExceptionHelper.checkNotNullArgument(property, "property");
            this.property = property;
        }

        @Override
        public ValueType getValue() {
            return property.getValue();
        }

        @Override
        public boolean isDefault() {
            return property.isDefault();
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            return property.addChangeListener(listener);
        }
    }
}
