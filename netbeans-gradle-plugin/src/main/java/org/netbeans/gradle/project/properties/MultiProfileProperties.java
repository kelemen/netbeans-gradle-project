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
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.util.NbFunction;
import org.w3c.dom.Element;

public final class MultiProfileProperties implements ActiveSettingsQueryEx {
    private final MutableProperty<List<ProjectProfileSettings>> currentProfileSettingsList;
    private final PropertySource<ProjectProfileSettings> currentProfileSettings;
    private final ChangeListenerManager currentProfileChangeListeners;

    public MultiProfileProperties(List<ProjectProfileSettings> initialProfiles) {
        this.currentProfileSettingsList = PropertyFactory.memPropertyConcurrent(
                CollectionsEx.readOnlyCopy(initialProfiles),
                SwingTaskExecutor.getStrictExecutor(false));

        this.currentProfileChangeListeners = GenericChangeListenerManager.getSwingNotifier();
        this.currentProfileSettings = new PropertySource<ProjectProfileSettings>() {
            @Override
            public ProjectProfileSettings getValue() {
                return tryGetCurrentProfileSettings();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return currentProfileChangeListeners.registerListener(listener);
            }
        };
    }

    @Override
    public Element getAuxConfigValue(DomElementKey key) {
        for (ProjectProfileSettings settings: currentProfileSettingsList.getValue()) {
            Element result = settings.getAuxConfigValue(key);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private ProjectProfileSettings tryGetCurrentProfileSettings() {
        List<ProjectProfileSettings> allProfileSettings = currentProfileSettingsList.getValue();
        return allProfileSettings.isEmpty() ? null : allProfileSettings.get(0);
    }

    @Override
    public PropertySource<ProjectProfileSettings> currentProfileSettings() {
        return currentProfileSettings;
    }

    public void setProfileSettings(List<? extends ProjectProfileSettings> newSettings) {
        List<ProjectProfileSettings> settingsCopy = CollectionsEx.readOnlyCopy(newSettings);

        ExceptionHelper.checkNotNullElements(settingsCopy, "newSettings");
        ExceptionHelper.checkArgumentInRange(settingsCopy.size(), 1, Integer.MAX_VALUE, "newSettings.size()");

        currentProfileSettingsList.setValue(settingsCopy);
        currentProfileChangeListeners.fireEventually();
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

    private static <ValueType> PropertySource<ValueType> mergedProperty(
            final PropertyDef<?, ValueType> propertyDef,
            final List<ProjectProfileSettings> profileList) {

        return new PropertySource<ValueType>() {
            @Override
            public ValueType getValue() {
                return mergePropertyValues(propertyDef, profileList);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                ExceptionHelper.checkNotNullArgument(listener, "listener");

                // Minor optimization for the default case
                if (profileList.size() == 1) {
                    MutableProperty<ValueType> property = profileList.get(0).getProperty(propertyDef);
                    return property.addChangeListener(listener);
                }
                else {
                    List<ListenerRef> refs = new ArrayList<>(profileList.size());
                    for (ProjectProfileSettings settings: profileList) {
                        MutableProperty<ValueType> property = settings.getProperty(propertyDef);
                        refs.add(property.addChangeListener(listener));
                    }
                    return ListenerRegistries.combineListenerRefs(refs);
                }
            }
        };
    }

    @Override
    public <ValueType> PropertySource<ValueType> getProperty(
            final PropertyDef<?, ValueType> propertyDef) {

        ExceptionHelper.checkNotNullArgument(propertyDef, "propertyDef");

        return NbProperties.propertyOfProperty(currentProfileSettingsList, new NbFunction<List<ProjectProfileSettings>, PropertySource<ValueType>>() {
            @Override
            public PropertySource<ValueType> call(List<ProjectProfileSettings> arg) {
                return mergedProperty(propertyDef, arg);
            }
        });
    }
}
