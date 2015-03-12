package org.netbeans.gradle.project.properties2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.event.OneShotListenerManager;
import org.jtrim.event.SimpleListenerRegistry;
import org.jtrim.event.UnregisteredListenerRef;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.event.NbListenerRefs;
import org.netbeans.gradle.project.properties.DomElementKey;
import org.w3c.dom.Element;

public final class MultiProfileProperties implements ActiveSettingsQueryEx {
    private final MutableProperty<List<ProjectProfileSettings>> currentProfileSettings;
    private final WaitableSignal loadedOnceSignal;
    private final OneShotListenerManager<Runnable, Void> loadedOnceListeners;

    public MultiProfileProperties() {
        this.currentProfileSettings = PropertyFactory.memPropertyConcurrent(
                Collections.<ProjectProfileSettings>emptyList(),
                SwingTaskExecutor.getStrictExecutor(false));
        this.loadedOnceSignal = new WaitableSignal();
        this.loadedOnceListeners = new OneShotListenerManager<>();
    }

    @Override
    public void waitForLoadedOnce(CancellationToken cancelToken) {
        loadedOnceSignal.waitSignal(cancelToken);
    }

    @Override
    public ListenerRef notifyWhenLoadedOnce(Runnable listener) {
        return loadedOnceListeners.registerOrNotifyListener(listener);
    }

    @Override
    public Element getAuxConfigValue(DomElementKey key) {
        for (ProjectProfileSettings settings: currentProfileSettings.getValue()) {
            Element result = settings.getAuxConfigValue(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    @Override
    public ProjectProfileSettings tryGetCurrentProfileSettings() {
        List<ProjectProfileSettings> allProfileSettings = currentProfileSettings.getValue();
        return allProfileSettings.isEmpty() ? null : allProfileSettings.get(0);
    }

    public void setProfileSettings(List<? extends ProjectProfileSettings> newSettings) {
        List<ProjectProfileSettings> settingsCopy = CollectionsEx.readOnlyCopy(newSettings);

        ExceptionHelper.checkNotNullElements(settingsCopy, "newSettings");
        ExceptionHelper.checkArgumentInRange(settingsCopy.size(), 1, Integer.MAX_VALUE, "newSettings.size()");

        currentProfileSettings.setValue(settingsCopy);
    }

    private static <ValueType> ValueType mergePropertyValues(
            PropertyDef<?, ValueType> propertyDef,
            List<ProjectProfileSettings> settings) {
        return mergePropertyValues(propertyDef, 0, settings);
    }

    private static <ValueType> ValueType mergePropertyValues(
            final PropertyDef<?, ValueType> propertyDef,
            final int settingsIndex,
            final List<ProjectProfileSettings> settings) {

        assert settings instanceof RandomAccess;

        int propertiesCount = settings.size() - settingsIndex;
        if (propertiesCount <= 0) {
            return null;
        }

        ProjectProfileSettings currentSettings = settings.get(settingsIndex);
        ValueType childValue = currentSettings.getProperty(propertyDef).getValue();
        if (propertiesCount <= 1) {
            return childValue;
        }

        return propertyDef.getValueMerger().mergeValues(childValue, new ValueReference<ValueType>() {
            @Override
            public ValueType getValue() {
                return mergePropertyValues(propertyDef, settingsIndex + 1, settings);
            }
        });
    }

    @Override
    public <ValueType> PropertySource<ValueType> getProperty(
            final PropertyDef<?, ValueType> propertyDef) {

        ExceptionHelper.checkNotNullArgument(propertyDef, "propertyDef");

        return new ProjectPropertyProxy<>(propertyDef, currentProfileSettings);
    }

    private static SimpleListenerRegistry<Runnable> asListenerRegistry(final PropertySource<?> property) {
        return new SimpleListenerRegistry<Runnable>() {
            @Override
            public ListenerRef registerListener(Runnable listener) {
                return property.addChangeListener(listener);
            }
        };
    }

    private static class ProjectPropertyProxy<ValueType>
    implements
            PropertySource<ValueType> {

        private final PropertyDef<?, ValueType> propertyDef;
        private final PropertySource<List<ProjectProfileSettings>> currentProfileSettings;

        public ProjectPropertyProxy(
                PropertyDef<?, ValueType> propertyDef,
                PropertySource<List<ProjectProfileSettings>> currentProfileSettings) {

            this.propertyDef = propertyDef;
            this.currentProfileSettings = currentProfileSettings;
        }


        @Override
        public ValueType getValue() {
            return mergePropertyValues(propertyDef, currentProfileSettings.getValue());
        }

        private void registerWithSubListener(Runnable listener, AtomicReference<ListenerRef> subListenerRef) {
            SimpleListenerRegistry<Runnable> mergedListeners = mergedPropertyListener(propertyDef);
            ListenerRef newRef = mergedListeners.registerListener(listener);
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

            final ListenerRef listenerRef = currentProfileSettings.addChangeListener(new Runnable() {
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

        private SimpleListenerRegistry<Runnable> mergedPropertyListener(final PropertyDef<?, ValueType> propertyDef) {
            final List<ProjectProfileSettings> allSettings = currentProfileSettings.getValue();
            // Minor optimization for the default case
            if (allSettings.size() == 1) {
                MutableProperty<ValueType> property = allSettings.get(0).getProperty(propertyDef);
                return asListenerRegistry(property);
            }

            return new SimpleListenerRegistry<Runnable>() {
                @Override
                public ListenerRef registerListener(Runnable listener) {
                    List<ListenerRef> refs = new ArrayList<>(allSettings.size());
                    for (ProjectProfileSettings settings: allSettings) {
                        MutableProperty<ValueType> property = settings.getProperty(propertyDef);
                        refs.add(property.addChangeListener(listener));
                    }
                    return ListenerRegistries.combineListenerRefs(refs);
                }
            };
        }
    }
}
