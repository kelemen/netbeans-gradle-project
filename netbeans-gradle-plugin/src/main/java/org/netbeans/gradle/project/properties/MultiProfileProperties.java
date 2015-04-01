package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.RandomAccess;
import org.jtrim.collections.CollectionsEx;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.util.NbFunction;
import org.w3c.dom.Element;

public final class MultiProfileProperties implements ActiveSettingsQueryEx {
    private final MutableProperty<List<SingleProfileSettingsEx>> currentProfileSettingsList;
    private final PropertySource<SingleProfileSettings> currentProfileSettings;
    private final PropertySource<SingleProfileSettingsEx> currentProfileSettingsEx;
    private final ChangeListenerManager currentProfileChangeListeners;

    public MultiProfileProperties(List<SingleProfileSettingsEx> initialProfiles) {
        this.currentProfileSettingsList = PropertyFactory.memPropertyConcurrent(
                CollectionsEx.readOnlyCopy(initialProfiles),
                SwingTaskExecutor.getStrictExecutor(false));

        this.currentProfileChangeListeners = GenericChangeListenerManager.getSwingNotifier();
        this.currentProfileSettingsEx = new PropertySource<SingleProfileSettingsEx>() {
            @Override
            public SingleProfileSettingsEx getValue() {
                return tryGetCurrentProfileSettings();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return currentProfileChangeListeners.registerListener(listener);
            }
        };

        // Just for type safety. An unsafe cast would do because of type erasure.
        this.currentProfileSettings = PropertyFactory.convert(currentProfileSettingsEx, new ValueConverter<SingleProfileSettingsEx, SingleProfileSettings>() {
            @Override
            public SingleProfileSettings convert(SingleProfileSettingsEx input) {
                return input;
            }
        });
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

    private SingleProfileSettingsEx tryGetCurrentProfileSettings() {
        List<SingleProfileSettingsEx> allProfileSettings = currentProfileSettingsList.getValue();
        return allProfileSettings.isEmpty() ? null : allProfileSettings.get(0);
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
        List<SingleProfileSettingsEx> settingsCopy = CollectionsEx.readOnlyCopy(newSettings);

        ExceptionHelper.checkNotNullElements(settingsCopy, "newSettings");
        ExceptionHelper.checkArgumentInRange(settingsCopy.size(), 1, Integer.MAX_VALUE, "newSettings.size()");

        currentProfileSettingsList.setValue(settingsCopy);
        currentProfileChangeListeners.fireEventually();
    }

    private static <ValueType> ValueType mergePropertyValues(
            PropertyDef<?, ValueType> propertyDef,
            List<SingleProfileSettingsEx> settings) {
        return mergePropertyValues(propertyDef, 0, settings);
    }

    private static <ValueType> ValueType mergePropertyValues(
            final PropertyDef<?, ValueType> propertyDef,
            final int settingsIndex,
            final List<SingleProfileSettingsEx> settings) {

        assert settings instanceof RandomAccess;

        int propertiesCount = settings.size() - settingsIndex;
        if (propertiesCount <= 0) {
            return null;
        }

        SingleProfileSettingsEx currentSettings = settings.get(settingsIndex);
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

    private static <ValueType> PropertySource<ValueType> mergedProperty(
            final PropertyDef<?, ValueType> propertyDef,
            final List<SingleProfileSettingsEx> profileList) {

        // Minor optimization for the default profile
        if (profileList.size() == 1) {
            return profileList.get(0).getProperty(propertyDef);
        }

        return new PropertySource<ValueType>() {
            @Override
            public ValueType getValue() {
                return mergePropertyValues(propertyDef, profileList);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                ExceptionHelper.checkNotNullArgument(listener, "listener");

                List<ListenerRef> refs = new ArrayList<>(profileList.size());
                for (SingleProfileSettingsEx settings: profileList) {
                    MutableProperty<ValueType> property = settings.getProperty(propertyDef);
                    refs.add(property.addChangeListener(listener));
                }
                return ListenerRegistries.combineListenerRefs(refs);
            }
        };
    }

    @Override
    public <ValueType> PropertySource<ValueType> getProperty(
            final PropertyDef<?, ValueType> propertyDef) {

        ExceptionHelper.checkNotNullArgument(propertyDef, "propertyDef");

        return NbProperties.propertyOfProperty(currentProfileSettingsList, new NbFunction<List<SingleProfileSettingsEx>, PropertySource<ValueType>>() {
            @Override
            public PropertySource<ValueType> apply(List<SingleProfileSettingsEx> arg) {
                return mergedProperty(propertyDef, arg);
            }
        });
    }
}
