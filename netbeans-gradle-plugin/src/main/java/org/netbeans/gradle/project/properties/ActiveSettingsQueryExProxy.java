package org.netbeans.gradle.project.properties;

import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.util.NbFunction;
import org.w3c.dom.Element;

public final class ActiveSettingsQueryExProxy implements ActiveSettingsQueryEx {
    private final PropertySource<ActiveSettingsQueryEx> wrappedRef;

    private final PropertySource<SingleProfileSettingsEx> currentProfileSettingsEx;
    private final PropertySource<SingleProfileSettings> currentProfileSettings;

    public ActiveSettingsQueryExProxy(PropertySource<ActiveSettingsQueryEx> wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");

        this.wrappedRef = wrapped;

        this.currentProfileSettingsEx = NbProperties.propertyOfProperty(wrapped, new NbFunction<ActiveSettingsQueryEx, PropertySource<SingleProfileSettingsEx>>() {
            @Override
            public PropertySource<SingleProfileSettingsEx> apply(ActiveSettingsQueryEx arg) {
                return arg.currentProfileSettingsEx();
            }
        });
        this.currentProfileSettings = NbProperties.propertyOfProperty(wrapped, new NbFunction<ActiveSettingsQueryEx, PropertySource<SingleProfileSettings>>() {
            @Override
            public PropertySource<SingleProfileSettings> apply(ActiveSettingsQueryEx arg) {
                return arg.currentProfileSettings();
            }
        });
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
        ExceptionHelper.checkNotNullArgument(propertyDef, "propertyDef");

        return NbProperties.propertyOfProperty(wrappedRef, new NbFunction<ActiveSettingsQueryEx, PropertySource<ValueType>>() {
            @Override
            public PropertySource<ValueType> apply(ActiveSettingsQueryEx arg) {
                return arg.getProperty(propertyDef);
            }
        });
    }

    @Override
    public PropertySource<SingleProfileSettings> currentProfileSettings() {
        return currentProfileSettings;
    }
}
