package org.netbeans.gradle.project.properties;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.util.NbFunction;

final class PropertyOfProperty<RootValue, SubValue> implements PropertySource<SubValue> {
    private final PropertySource<? extends RootValue> rootSrc;
    private final NbFunction<? super RootValue, ? extends PropertySource<SubValue>> subPropertyGetter;

    public PropertyOfProperty(
            PropertySource<? extends RootValue> rootSrc,
            NbFunction<? super RootValue, ? extends PropertySource<SubValue>> subPropertyGetter) {
        this.rootSrc = Objects.requireNonNull(rootSrc, "rootSrc");
        this.subPropertyGetter = Objects.requireNonNull(subPropertyGetter, "subPropertyGetter");
    }

    private PropertySource<SubValue> getSubProperty() {
        RootValue rootValue = rootSrc.getValue();
        return subPropertyGetter.apply(rootValue);
    }

    @Override
    public SubValue getValue() {
        return getSubProperty().getValue();
    }

    private void registerWithSubListener(Runnable listener, AtomicReference<ListenerRef> subListenerRef) {
        ListenerRef newRef = getSubProperty().addChangeListener(listener);
        ListenerRef prevRef = subListenerRef.getAndSet(newRef);
        if (prevRef != null) {
            prevRef.unregister();
        }
        else {
            subListenerRef.compareAndSet(newRef, null);
            newRef.unregister();
        }
    }

    @Override
    public ListenerRef addChangeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        AtomicReference<ListenerRef> subListenerRef = new AtomicReference<>(ListenerRefs.unregistered());
        // subListenerRef.get() == null means that the the client
        // unregistered its listener and therefore, we must no longer
        // register listeners. That is, once this property is null, we may
        // never set it.

        final ListenerRef listenerRef = rootSrc.addChangeListener(() -> {
            registerWithSubListener(listener, subListenerRef);
            listener.run();
        });

        registerWithSubListener(listener, subListenerRef);

        return () -> {
            listenerRef.unregister();
            ListenerRef subRef = subListenerRef.getAndSet(null);
            if (subRef != null) {
                subRef.unregister();
            }
        };
    }
}
