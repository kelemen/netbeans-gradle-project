package org.netbeans.gradle.project.properties;

import java.util.Collection;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.properties2.ProfileKey;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.w3c.dom.Element;

public final class GradleAuxiliaryConfiguration implements AuxiliaryConfiguration {
    private final LazyAuxProperties sharedConfig;
    private final LazyAuxProperties privateConfig;

    public GradleAuxiliaryConfiguration(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        // This object is created before the lookup of the project is created
        // and therefore we cannot yet request the profiles because the profile
        // retrieval requires the lookup to be laready built.
        this.sharedConfig = new LazyAuxProperties(project, null);
        this.privateConfig = new LazyAuxProperties(project, ProfileKey.PRIVATE_PROFILE);
    }

    private LazyAuxProperties getConfig(boolean shared) {
        return shared ? sharedConfig : privateConfig;
    }

    @Override
    public Element getConfigurationFragment(String elementName, String namespace, boolean shared) {
        return getConfig(shared).getAuxConfigValue(new DomElementKey(elementName, namespace));
    }

    @Override
    public void putConfigurationFragment(Element fragment, boolean shared) throws IllegalArgumentException {
        LazyAuxProperties config = getConfig(shared);
        config.setAuxConfigValue(keyFromElement(fragment), fragment);
    }

    @Override
    public boolean removeConfigurationFragment(
            String elementName,
            String namespace,
            boolean shared) throws IllegalArgumentException {

        LazyAuxProperties config = getConfig(shared);
        return config.setAuxConfigValue(new DomElementKey(elementName, namespace), null);
    }

    private static DomElementKey keyFromElement(Element fragment) {
        ExceptionHelper.checkNotNullArgument(fragment, "fragment");
        return new DomElementKey(fragment.getTagName(), fragment.getNamespaceURI());
    }

    public Collection<DomElementKey> getConfigElements(boolean shared) {
        return getConfig(shared).getAuxConfigKeys();
    }
}
