package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.RandomAccess;
import java.util.function.Function;
import org.jtrim2.collections.CollectionsEx;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.jtrim2.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.SingleProfileSettings;
import org.netbeans.gradle.project.api.config.ValueMerger;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.w3c.dom.Element;

public final class MultiProfileProperties implements ActiveSettingsQueryEx {
    private final MutableProperty<List<SingleProfileSettingsEx>> currentProfileSettingsList;
    private final PropertySource<SingleProfileSettings> currentProfileSettings;
    private final PropertySource<SingleProfileSettingsEx> currentProfileSettingsEx;
    private final ChangeListenerManager currentProfileChangeListeners;

    public MultiProfileProperties(List<SingleProfileSettingsEx> initialProfiles) {
        this.currentProfileSettingsList = PropertyFactory.memPropertyConcurrent(
                CollectionsEx.readOnlyCopy(copySettings(initialProfiles)),
                SwingExecutors.getStrictExecutor(false));

        this.currentProfileChangeListeners = GenericChangeListenerManager.getSwingNotifier();
        this.currentProfileSettingsEx = new PropertySource<SingleProfileSettingsEx>() {
            @Override
            public SingleProfileSettingsEx getValue() {
                return getCurrentProfileSettings();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return currentProfileChangeListeners.registerListener(listener);
            }
        };

        // Just for type safety. An unsafe cast would do because of type erasure.
        this.currentProfileSettings = PropertyFactory.convert(currentProfileSettingsEx, input -> input);
    }

    @Override
    public Element getAuxConfigValue(DomElementKey key) {
        for (SingleProfileSettingsEx settings: currentProfileSettingsList.getValue()) {
            Element result = settings.getAuxConfigValue(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static List<SingleProfileSettingsEx> copySettings(List<? extends SingleProfileSettingsEx> settings) {
        List<SingleProfileSettingsEx> result = CollectionsEx.readOnlyCopy(settings);

        ExceptionHelper.checkNotNullElements(result, "settings");
        ExceptionHelper.checkArgumentInRange(result.size(), 1, Integer.MAX_VALUE, "settings.size()");
        return result;
    }

    private SingleProfileSettingsEx getCurrentProfileSettings() {
        List<SingleProfileSettingsEx> allProfileSettings = currentProfileSettingsList.getValue();
        if (allProfileSettings.isEmpty()) {
            throw new AssertionError("No profile was set.");
        }
        return allProfileSettings.get(0);
    }

    @Override
    public PropertySource<SingleProfileSettingsEx> currentProfileSettingsEx() {
        return currentProfileSettingsEx;
    }

    @Override
    public PropertySource<SingleProfileSettings> currentProfileSettings() {
        return currentProfileSettings;
    }

    public void setProfileSettings(List<? extends SingleProfileSettingsEx> newSettings) {
        currentProfileSettingsList.setValue(copySettings(newSettings));
        currentProfileChangeListeners.fireEventually();
    }

    private static <ValueType> ValueType mergePropertyValues(
            ValueMerger<ValueType> valueMerger,
            List<? extends PropertySource<ValueType>> properties) {
        return mergePropertyValues(0, valueMerger, properties);
    }

    private static <ValueType> ValueType mergePropertyValues(
            final int settingsIndex,
            final ValueMerger<ValueType> valueMerger,
            final List<? extends PropertySource<ValueType>> properties) {

        assert properties instanceof RandomAccess;

        int propertiesCount = properties.size() - settingsIndex;
        if (propertiesCount <= 0) {
            return null;
        }

        ValueType childValue = properties.get(settingsIndex).getValue();
        if (propertiesCount <= 1) {
            return childValue;
        }

        return valueMerger.mergeValues(childValue, () -> {
            return mergePropertyValues(settingsIndex + 1, valueMerger, properties);
        });
    }

    private static <ValueType> PropertySource<ValueType> mergedProperty(
            PropertyDef<?, ValueType> propertyDef,
            List<SingleProfileSettingsEx> profileList) {

        // Minor optimization for the default profile
        if (profileList.size() == 1) {
            return profileList.get(0).getProperty(propertyDef);
        }

        final List<PropertySource<ValueType>> properties = new ArrayList<>();
        for (SingleProfileSettingsEx profile: profileList) {
            properties.add(profile.getProperty(propertyDef));
        }

        final ValueMerger<ValueType> valueMerger = propertyDef.getValueMerger();

        return new PropertySource<ValueType>() {
            @Override
            public ValueType getValue() {
                return mergePropertyValues(valueMerger, properties);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                Objects.requireNonNull(listener, "listener");

                List<ListenerRef> refs = new ArrayList<>(properties.size());
                for (PropertySource<ValueType> property: properties) {
                    refs.add(property.addChangeListener(listener));
                }
                return ListenerRefs.combineListenerRefs(refs);
            }
        };
    }

    @Override
    public <ValueType> PropertySource<ValueType> getProperty(PropertyDef<?, ValueType> propertyDef) {
        return PropertyFactory.propertyOfProperty(currentProfileSettingsList, new CachingPropertyMerger<>(propertyDef));
    }

    private static class CachingPropertyMerger<Value> implements Function<List<SingleProfileSettingsEx>, PropertySource<Value>> {
        private final PropertyDef<?, Value> propertyDef;
        private volatile CachedMergedProperty<Value> cachedValue;

        public CachingPropertyMerger(PropertyDef<?, Value> propertyDef) {
            this.propertyDef = Objects.requireNonNull(propertyDef, "propertyDef");
            this.cachedValue = null;
        }

        @Override
        public PropertySource<Value> apply(List<SingleProfileSettingsEx> arg) {
            CachedMergedProperty<Value> result = cachedValue;
            if (result == null || !result.isSame(arg)) {
                result = new CachedMergedProperty<>(propertyDef, arg);
            }

            return result.getProperty();
        }
    }

    private static final class CachedMergedProperty<Value> {
        private final List<SingleProfileSettingsEx> settings;
        private final PropertySource<Value> property;

        public CachedMergedProperty(
                PropertyDef<?, Value> propertyDef,
                List<SingleProfileSettingsEx> settings) {

            this.settings = CollectionsEx.readOnlyCopy(settings);
            this.property = mergedProperty(propertyDef, this.settings);
        }

        public boolean isSame(List<SingleProfileSettingsEx> testedSettings) {
            return Objects.equals(this.settings, testedSettings);
        }

        public PropertySource<Value> getProperty() {
            return property;
        }
    }
}
