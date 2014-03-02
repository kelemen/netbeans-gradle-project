package org.netbeans.gradle.project.properties;

import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.event.NbListenerRef;
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
    public NbListenerRef addChangeListener(final Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final ChangeListener changeListener = new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                listener.run();
            }
        };

        source.addChangeListener(changeListener);
        return new NbListenerRef() {
            private volatile boolean registered = true;

            @Override
            public boolean isRegistered() {
                return registered;
            }

            @Override
            public void unregister() {
                source.removeChangeListener(changeListener);
                registered = false;
            }
        };
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
        public void addChangeListener(ChangeListener listener) {
            property.addChangeListener(listener);
        }

        @Override
        public void removeChangeListener(ChangeListener listener) {
            property.removeChangeListener(listener);
        }
    }
}
