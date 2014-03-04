package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;
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

    public NbPropertySourceWrapper(MutableProperty<ValueType> property) {
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

    private static class PropertySourceWrapper<ValueType> implements PropertySource<ValueType> {
        private final MutableProperty<ValueType> property;

        public PropertySourceWrapper(MutableProperty<ValueType> property) {
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
