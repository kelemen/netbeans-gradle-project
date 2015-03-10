package org.netbeans.gradle.project.properties2;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.RandomAccess;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.PropertySourceProxy;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;

public final class MultiProfileProperties {
    private final MutableProperty<List<ProjectProfileSettings>> currentProfileSettings;

    public MultiProfileProperties() {
        this.currentProfileSettings = PropertyFactory.memPropertyConcurrent(
                Collections.<ProjectProfileSettings>emptyList(),
                SwingTaskExecutor.getStrictExecutor(false));
    }

    public void setProfileSettings(List<? extends ProjectProfileSettings> newSettings) {
        List<ProjectProfileSettings> settingsCopy = CollectionsEx.readOnlyCopy(newSettings);
        ExceptionHelper.checkNotNullElements(settingsCopy, "newSettings");

        currentProfileSettings.setValue(settingsCopy);
    }

    private static <ValueType> ValueType mergeProperties(
            final ValueMerger<ValueType> valueMerger,
            final int propertyIndex,
            final List<PropertySource<ValueType>> properties) {

        assert properties instanceof RandomAccess;

        int propertiesCount = properties.size() - propertyIndex;
        if (propertiesCount <= 0) {
            return null;
        }

        ValueType childValue = properties.get(propertyIndex).getValue();
        if (propertiesCount <= 1) {
            return childValue;
        }

        return valueMerger.mergeValues(childValue, new ValueReference<ValueType>() {
            @Override
            public ValueType getValue() {
                return mergeProperties(valueMerger, propertyIndex + 1, properties);
            }
        });
    }

    private static <ValueType> ValueType mergeProperties(
            ValueMerger<ValueType> valueMerger,
            List<PropertySource<ValueType>> properties) {
        return mergeProperties(valueMerger, 0, properties);
    }

    private <ValueType> PropertySource<ValueType> mergedProperties(PropertyDef<?, ValueType> propertyDef) {
        List<ProjectProfileSettings> allSettings = currentProfileSettings.getValue();
        final List<PropertySource<ValueType>> properties = new ArrayList<>(allSettings.size());
        for (ProjectProfileSettings settings: allSettings) {
            properties.add(settings.getProperty(propertyDef));
        }

        final ValueMerger<ValueType> valueMerger = propertyDef.getValueMerger();
        return new PropertySource<ValueType>() {
            @Override
            public ValueType getValue() {
                return mergeProperties(valueMerger, properties);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                List<ListenerRef> refs = new ArrayList<>(properties.size());
                for (PropertySource<?> property: properties) {
                    refs.add(property.addChangeListener(listener));
                }
                return ListenerRegistries.combineListenerRefs(refs);
            }
        };
    }

    public <ValueKey, ValueType> AcquiredPropertySource<ValueType> acquireProperty(
            final PropertyDef<ValueKey, ValueType> propertyDef) {

        ExceptionHelper.checkNotNullArgument(propertyDef, "propertyDef");

        final PropertySourceProxy<ValueType> result
                = PropertyFactory.proxySource(PropertyFactory.<ValueType>constSource(null));

        final ListenerRef profileRef = currentProfileSettings.addChangeListener(new Runnable() {
            @Override
            public void run() {
                result.replaceSource(mergedProperties(propertyDef));
            }
        });
        result.replaceSource(mergedProperties(propertyDef));

        return new AcquiredPropertySource<ValueType>() {
            @Override
            public ValueType getValue() {
                return result.getValue();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return result.addChangeListener(listener);
            }

            @Override
            public void close() {
                profileRef.unregister();
            }
        };
    }
}
