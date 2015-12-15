package org.netbeans.gradle.project.properties;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
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

/**
 * Defines a complete project property definition with every fallbacks.
 * An instance of this class might be created for each property to be
 * conveniently usable by actions needing that property.
 *
 * @param <ValueType> the type of the value of the property
 *
 * @see ActiveSettingsQuery
 * @see org.netbeans.gradle.project.api.config.ProjectSettingsProvider
 */
public final class PropertyReference<ValueType> {
    private final PropertyDef<?, ValueType> propertyDef;
    private final ActiveSettingsQuery activeSettingsQuery;
    private final PropertySource<ValueType> activeSource;

    /**
     * Creates a new {@code PropertyReference} with the given properties.
     * <P>
     * The fallback value for this property will be {@code null}.
     *
     * @param propertyDef the {@code PropertyDef} defining how to retrieve and
     *   store this property. This argument cannot be {@code null}.
     * @param activeSettingsQuery the {@code ActiveSettingsQuery} used to
     *   access the given property. This argument cannot be {@code null}.
     */
    public PropertyReference(PropertyDef<?, ValueType> propertyDef, ActiveSettingsQuery activeSettingsQuery) {
        this(propertyDef, activeSettingsQuery, PropertyFactory.<ValueType>constSource(null));
    }

    /**
     * Creates a new {@code PropertyReference} with the given properties.
     *
     * @param propertyDef the {@code PropertyDef} defining how to retrieve and
     *   store this property. This argument cannot be {@code null}.
     * @param activeSettingsQuery the {@code ActiveSettingsQuery} used to
     *   access the given property. This argument cannot be {@code null}.
     * @param defaultValue the fallback value of this property. That is,
     *   the merging strategy defined in the specified {@code PropertyDef}
     *   will be used with the value of this property as parent. This argument
     *   cannot be {@code null}.
     */
    public PropertyReference(
            @Nonnull PropertyDef<?, ValueType> propertyDef,
            @Nonnull ActiveSettingsQuery activeSettingsQuery,
            @Nonnull PropertySource<? extends ValueType> defaultValue) {
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

    /**
     * Returns the {@code PropertySource} providing the value of the property
     * considering all the fallback values.
     *
     * @return the {@code PropertySource} providing the value of the property
     *   considering all the fallback values. This method never returns
     *   {@code null}.
     */
    @Nonnull
    public PropertySource<ValueType> getActiveSource() {
        return activeSource;
    }

    /**
     * Returns the current value of this property considering all the fallback
     * values.
     *
     * @return the current value of this property considering all the fallback
     *   values. This method may only return {@code null} if the value of this
     *   property can be {@code null}, considering the fallback values and
     *   fallback strategy.
     */
    public ValueType getActiveValue() {
        return activeSource.getValue();
    }

    /**
     * Returns the {@code PropertyDef} defining how to retrieve and
     * store this property. This is the same object as specified at construction
     * time.
     *
     * @return the {@code PropertyDef} defining how to retrieve and
     *   store this property.
     */
    @Nonnull
    public PropertyDef<?, ValueType> getPropertyDef() {
        return propertyDef;
    }

    /**
     * Returns the value of the property of the current profile without
     * considering fallback values.
     *
     * @return the value of the property of the current profile without
     *   considering fallback values. This method may return {@code null},
     *   if the value of this property is {@code null}.
     */
    @Nullable
    public ValueType tryGetValueWithoutFallback() {
        MutableProperty<ValueType> property = tryGetForActiveProfile();
        return property != null ? property.getValue() : null;
    }

    /**
     * Sets the value of this property for the currently selected profile to
     * the specified value.
     *
     * @param value the value to which this property is set. This argument
     *   can be {@code null}. Setting a property to {@code null} means setting
     *   it to the default value.
     * @return always returs {@code true}
     */
    public boolean trySetValue(ValueType value) {
        // FIXME: This property should never be null, so the check is pointless.
        MutableProperty<ValueType> property = tryGetForActiveProfile();
        if (property == null) {
            return false;
        }

        property.setValue(value);
        return true;
    }

    /**
     * Returns the {@code MutableProperty} which can be used to access and alter
     * the value of this property not considering fallback values. That is,
     * through this property, you can set the value of this property in the
     * currently selected profile.
     * <P>
     * Even if a new profile gets selected, the returned property will still
     * represent the originally chosen profile.
     *
     * @return the {@code MutableProperty} which can be used to access and alter
     *   the value of this property not considering fallback values. This method
     *   never returns {@code null}.
     */
    @Nullable
    public MutableProperty<ValueType> tryGetForActiveProfile() {
         // FIXME: This property should never be null, so the check is pointless.
        SingleProfileSettings settings = activeSettingsQuery.currentProfileSettings().getValue();
        return settings != null ? forProfile(settings) : null;
    }

    /**
     * Returns the {@code MutableProperty} which can be used to access and alter
     * the value of this property from the given profile.
     *
     * @returnthe {@code MutableProperty} which can be used to access and alter
     *   the value of this property from the given profile. This method
     *   never returns {@code null}.
     */
    @Nonnull
    public MutableProperty<ValueType> forProfile(SingleProfileSettings settings) {
        return settings.getProperty(propertyDef);
    }
}
