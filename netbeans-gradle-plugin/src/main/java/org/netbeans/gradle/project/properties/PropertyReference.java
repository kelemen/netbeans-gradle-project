package org.netbeans.gradle.project.properties;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.SingleProfileSettings;
import org.netbeans.gradle.project.api.config.ValueMerger;
import org.netbeans.gradle.project.api.config.ValueReference;

public final class PropertyReference<ValueType> {
    private final PropertyDef<?, ValueType> propertyDef;
    private final ActiveSettingsQuery activeSettingsQuery;
    private final PropertySource<ValueType> activeSource;

    public PropertyReference(PropertyDef<?, ValueType> propertyDef, ActiveSettingsQuery activeSettingsQuery) {
        this(propertyDef, activeSettingsQuery, PropertyFactory.<ValueType>constSource(null));
    }

    public PropertyReference(
            PropertyDef<?, ValueType> propertyDef,
            ActiveSettingsQuery activeSettingsQuery,
            PropertySource<? extends ValueType> defaultValue) {
        ExceptionHelper.checkNotNullArgument(propertyDef, "propertyDef");
        ExceptionHelper.checkNotNullArgument(activeSettingsQuery, "activeSettingsQuery");
        ExceptionHelper.checkNotNullArgument(defaultValue, "defaultValue");

        this.propertyDef = propertyDef;
        this.activeSettingsQuery = activeSettingsQuery;
        this.activeSource = mergeProperties(
                activeSettingsQuery.getProperty(propertyDef),
                defaultValue,
                propertyDef.getValueMerger());
    }

    private static <ValueType> PropertySource<ValueType> mergeProperties(
            final PropertySource<? extends ValueType> src,
            final PropertySource<? extends ValueType> fallback,
            final ValueMerger<ValueType> valueMerger) {
        assert src != null;
        assert fallback != null;
        assert valueMerger != null;

        final ValueReference<ValueType> parentValueRef = new ValueReference<ValueType>() {
            @Override
            public ValueType getValue() {
                return fallback.getValue();
            }
        };

        return new PropertySource<ValueType>() {
            @Override
            public ValueType getValue() {
                return valueMerger.mergeValues(src.getValue(), parentValueRef);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                ListenerRef ref1 = src.addChangeListener(listener);
                ListenerRef ref2 = fallback.addChangeListener(listener);

                return ListenerRegistries.combineListenerRefs(ref1, ref2);
            }
        };
    }

    public PropertySource<ValueType> getActiveSource() {
        return activeSource;
    }

    public ValueType getActiveValue() {
        return activeSource.getValue();
    }

    public PropertyDef<?, ValueType> getPropertyDef() {
        return propertyDef;
    }

    public ValueType tryGetValueWithoutFallback() {
        MutableProperty<ValueType> property = tryGetForActiveProfile();
        return property != null ? property.getValue() : null;
    }

    public boolean trySetValue(ValueType value) {
        MutableProperty<ValueType> property = tryGetForActiveProfile();
        if (property == null) {
            return false;
        }

        property.setValue(value);
        return true;
    }

    public MutableProperty<ValueType> tryGetForActiveProfile() {
        SingleProfileSettings settings = activeSettingsQuery.currentProfileSettings().getValue();
        return settings != null ? forProfile(settings) : null;
    }

    public MutableProperty<ValueType> forProfile(SingleProfileSettings settings) {
        return settings.getProperty(propertyDef);
    }
}
