package org.netbeans.gradle.project.properties;

import java.util.Collection;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.w3c.dom.Element;

public final class GradleAuxiliaryConfiguration implements AuxiliaryConfiguration {
    private final NbGradleProject project;
    private final AtomicReference<ProjectProfileSettings> sharedConfigRef;
    private final AtomicReference<ProjectProfileSettings> privateConfigRef;

    public GradleAuxiliaryConfiguration(NbGradleProject project) {
        ExceptionHelper.checkNotNullArgument(project, "project");

        this.project = project;

        // This object is created before the lookup of the project is created
        // and therefore we cannot yet request the profiles because the profile
        // retrieval requires the lookup to be laready built.
        this.sharedConfigRef = new AtomicReference<>(null);
        this.privateConfigRef = new AtomicReference<>(null);
    }

    private static ProjectProfileSettings getSharedProperties(NbGradleProject project) {
        return project.getPropertiesForProfile(null);
    }

    private static ProjectProfileSettings getPrivateProperties(NbGradleProject project) {
        return project.getPrivateProfile();
    }

    private ProjectProfileSettings getSharedProperties() {
        ProjectProfileSettings result = sharedConfigRef.get();
        if (result == null) {
            result = getSharedProperties(project);
            if (!sharedConfigRef.compareAndSet(null, result)) {
                result = sharedConfigRef.get();
            }
        }

        return result;
    }

    private ProjectProfileSettings getPrivateProperties() {
        ProjectProfileSettings result = privateConfigRef.get();
        if (result == null) {
            result = getPrivateProperties(project);
            if (!privateConfigRef.compareAndSet(null, result)) {
                result = privateConfigRef.get();
            }
        }

        return result;
    }

    private ProjectProfileSettings getConfig(boolean shared) {
        ProjectProfileSettings result = shared ? getSharedProperties() : getPrivateProperties();
        result.ensureLoadedAndWait();
        return result;
    }

    @Override
    public Element getConfigurationFragment(String elementName, String namespace, boolean shared) {
        return getConfig(shared).getAuxConfigValue(new DomElementKey(elementName, namespace));
    }

    @Override
    public void putConfigurationFragment(Element fragment, boolean shared) throws IllegalArgumentException {
        ProjectProfileSettings config = getConfig(shared);
        config.setAuxConfigValue(keyFromElement(fragment), fragment);
    }

    @Override
    public boolean removeConfigurationFragment(
            String elementName,
            String namespace,
            boolean shared) throws IllegalArgumentException {

        ProjectProfileSettings config = getConfig(shared);
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
