package org.netbeans.gradle.project.properties;

import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.util.NbFunction;

final class PropertyOfProperty<RootValue, SubValue> implements PropertySource<SubValue> {
    private final PropertySource<? extends RootValue> rootSrc;
    private final NbFunction<? super RootValue, ? extends PropertySource<SubValue>> subPropertyGetter;

    public PropertyOfProperty(
            PropertySource<? extends RootValue> rootSrc,
            NbFunction<? super RootValue, ? extends PropertySource<SubValue>> subPropertyGetter) {
        ExceptionHelper.checkNotNullArgument(subPropertyGetter, "subPropertyGetter");
        ExceptionHelper.checkNotNullArgument(rootSrc, "rootSrc");

        this.rootSrc = rootSrc;
        this.subPropertyGetter = subPropertyGetter;
    }

    private PropertySource<SubValue> getSubProperty() {
        RootValue rootValue = rootSrc.getValue();
        return subPropertyGetter.call(rootValue);
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
    public ListenerRef addChangeListener(final Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");
        final AtomicReference<ListenerRef> subListenerRef
                = new AtomicReference<ListenerRef>(UnregisteredListenerRef.INSTANCE);
        // subListenerRef.get() == null means that the the client
        // unregistered its listener and therefore, we must no longer
        // register listeners. That is, once this property is null, we may
        // never set it.

        final ListenerRef listenerRef = rootSrc.addChangeListener(new Runnable() {
            @Override
            public void run() {
                registerWithSubListener(listener, subListenerRef);
                listener.run();
            }
        });

        registerWithSubListener(listener, subListenerRef);

        return NbListenerRefs.fromRunnable(new Runnable() {
            @Override
            public void run() {
                listenerRef.unregister();
                ListenerRef subRef = subListenerRef.getAndSet(null);
                if (subRef != null) {
                    subRef.unregister();
                }
            }
        });
    }

}
