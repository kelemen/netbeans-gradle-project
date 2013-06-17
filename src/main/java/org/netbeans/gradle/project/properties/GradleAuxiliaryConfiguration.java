package org.netbeans.gradle.project.properties;

import java.util.Collection;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.w3c.dom.Element;

public final class GradleAuxiliaryConfiguration implements AuxiliaryConfiguration {
    private final SingleStoreAuxConfig sharedConfig;
    private final SingleStoreAuxConfig privateConfig;

    public GradleAuxiliaryConfiguration(final NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");

        this.sharedConfig = new SingleStoreAuxConfig(
                new ProjectPropertiesStorage(getSharedProperties(project)));
        this.privateConfig = new SingleStoreAuxConfig(
                new ProjectPropertiesStorage(getPrivateProperties(project)));
    }

    private static ProjectProperties getSharedProperties(NbGradleProject project) {
        return project.getPropertiesForProfile(null, false, null);
    }

    private static ProjectProperties getPrivateProperties(NbGradleProject project) {
        return project.getPrivateProperties();
    }

    private SingleStoreAuxConfig getConfig(boolean shared) {
        return shared ? sharedConfig : privateConfig;
    }

    @Override
    public Element getConfigurationFragment(String elementName, String namespace, boolean shared) {
        return getConfig(shared).getConfigurationFragment(elementName, namespace);
    }

    @Override
    public void putConfigurationFragment(Element fragment, boolean shared) throws IllegalArgumentException {
        getConfig(shared).putConfigurationFragment(fragment);
    }

    @Override
    public boolean removeConfigurationFragment(
            final String elementName,
            final String namespace,
            boolean shared) throws IllegalArgumentException {

        return getConfig(shared).removeConfigurationFragment(elementName, namespace);
    }

    public Collection<DomElementKey> getConfigElements(boolean shared) {
        return getConfig(shared).getConfigElements();
    }

    private static final class ProjectPropertiesStorage
    implements
            SingleStoreAuxConfig.AuxConfigStorage {

        private final ProjectProperties properties;

        public ProjectPropertiesStorage(ProjectProperties properties) {
            if (properties == null) throw new NullPointerException("properties");
            this.properties = properties;
        }

        @Override
        public AuxConfigProperty getAuxConfig(String elementName, String namespace) {
            return getAuxConfig(elementName, namespace);
        }

        @Override
        public Collection<AuxConfigProperty> getAllAuxConfigs() {
            return properties.getAllAuxConfigs();
        }
    }
}
