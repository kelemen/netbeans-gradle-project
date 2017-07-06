package org.netbeans.gradle.project.properties;

import java.util.Collection;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import org.netbeans.gradle.project.api.config.ProfileKey;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.w3c.dom.Element;

public final class GradleAuxiliaryConfiguration implements AuxiliaryConfiguration {
    private final ProjectProfileLoader configManager;
    private final AtomicReference<SingleProfileSettingsEx> sharedConfigRef;
    private final AtomicReference<SingleProfileSettingsEx> privateConfigRef;

    public GradleAuxiliaryConfiguration(ProjectProfileLoader configManager) {
        this.configManager = Objects.requireNonNull(configManager, "configManager");

        // This object is created before the lookup of the project is created
        // and therefore we cannot yet request the profiles because the profile
        // retrieval requires the lookup to be laready built.
        this.sharedConfigRef = new AtomicReference<>(null);
        this.privateConfigRef = new AtomicReference<>(null);
    }

    private SingleProfileSettingsEx getSharedProperties() {
        SingleProfileSettingsEx result = sharedConfigRef.get();
        if (result == null) {
            result = configManager.loadPropertiesForProfile(ProfileKey.DEFAULT_PROFILE);
            if (!sharedConfigRef.compareAndSet(null, result)) {
                result = sharedConfigRef.get();
            }
        }

        return result;
    }

    private SingleProfileSettingsEx getPrivateProperties() {
        SingleProfileSettingsEx result = privateConfigRef.get();
        if (result == null) {
            result = configManager.loadPropertiesForProfile(ProfileKey.PRIVATE_PROFILE);
            if (!privateConfigRef.compareAndSet(null, result)) {
                result = privateConfigRef.get();
            }
        }

        return result;
    }

    private SingleProfileSettingsEx getConfig(boolean shared) {
        return shared ? getSharedProperties() : getPrivateProperties();
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
