package org.netbeans.gradle.project.properties;

import java.util.Objects;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.SingleProfileSettings;
import org.w3c.dom.Element;

public final class ActiveSettingsQueryExProxy implements ActiveSettingsQueryEx {
    private final PropertySource<ActiveSettingsQueryEx> wrappedRef;

    private final PropertySource<SingleProfileSettingsEx> currentProfileSettingsEx;
    private final PropertySource<SingleProfileSettings> currentProfileSettings;

    public ActiveSettingsQueryExProxy(PropertySource<ActiveSettingsQueryEx> wrapped) {
        this.wrappedRef = Objects.requireNonNull(wrapped, "wrapped");

        this.currentProfileSettingsEx = PropertyFactory.propertyOfProperty(wrapped, ActiveSettingsQueryEx::currentProfileSettingsEx);
        this.currentProfileSettings = PropertyFactory.propertyOfProperty(wrapped, ActiveSettingsQuery::currentProfileSettings);
    }

    private ActiveSettingsQueryEx getWrapped() {
        return wrappedRef.getValue();
    }

    @Override
    public Element getAuxConfigValue(DomElementKey key) {
        return getWrapped().getAuxConfigValue(key);
    }

    @Override
    public PropertySource<SingleProfileSettingsEx> currentProfileSettingsEx() {
        return currentProfileSettingsEx;
    }

    @Override
    public <ValueType> PropertySource<ValueType> getProperty(final PropertyDef<?, ValueType> propertyDef) {
        Objects.requireNonNull(propertyDef, "propertyDef");

        return PropertyFactory.propertyOfProperty(wrappedRef, arg -> arg.getProperty(propertyDef));
    }

    @Override
    public PropertySource<SingleProfileSettings> currentProfileSettings() {
        return currentProfileSettings;
    }
}
