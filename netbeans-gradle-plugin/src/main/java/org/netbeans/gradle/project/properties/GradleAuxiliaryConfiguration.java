package org.netbeans.gradle.project.properties;

import java.util.Collection;
import java.util.Objects;
import java.util.function.Supplier;
import org.jtrim2.utils.LazyValues;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.w3c.dom.Element;

public final class GradleAuxiliaryConfiguration implements AuxiliaryConfiguration {
    private final Supplier<SingleProfileSettingsEx> sharedConfigRef;
    private final Supplier<SingleProfileSettingsEx> privateConfigRef;

    public GradleAuxiliaryConfiguration(ProjectProfileLoader configManager) {
        Objects.requireNonNull(configManager, "configManager");

        // This object is created before the lookup of the project is created
        // and therefore we cannot yet request the profiles because the profile
        // retrieval requires the lookup to be already built.
        this.sharedConfigRef = LazyValues.lazyValue(() -> {
            return configManager.loadPropertiesForProfile(ProfileKey.DEFAULT_PROFILE);
        });
        this.privateConfigRef = LazyValues.lazyValue(() -> {
            return configManager.loadPropertiesForProfile(ProfileKey.PRIVATE_PROFILE);
        });
    }

    private SingleProfileSettingsEx getConfig(boolean shared) {
        return shared
                ? sharedConfigRef.get()
                : privateConfigRef.get();
    }

    @Override
    public Element getConfigurationFragment(String elementName, String namespace, boolean shared) {
        return getConfig(shared).getAuxConfigValue(new DomElementKey(elementName, namespace));
    }

    @Override
    public void putConfigurationFragment(Element fragment, boolean shared) throws IllegalArgumentException {
        SingleProfileSettingsEx config = getConfig(shared);
        config.setAuxConfigValue(keyFromElement(fragment), fragment);
    }

    @Override
    public boolean removeConfigurationFragment(
            String elementName,
            String namespace,
            boolean shared) throws IllegalArgumentException {

        SingleProfileSettingsEx config = getConfig(shared);
        return config.setAuxConfigValue(new DomElementKey(elementName, namespace), null);
    }

    private static DomElementKey keyFromElement(Element fragment) {
        Objects.requireNonNull(fragment, "fragment");
        return new DomElementKey(fragment.getTagName(), fragment.getNamespaceURI());
    }

    public Collection<DomElementKey> getConfigElements(boolean shared) {
        return getConfig(shared).getAuxConfigKeys();
    }
}
