package org.netbeans.gradle.project.properties;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.w3c.dom.Element;

public final class GradleAuxiliaryConfiguration implements AuxiliaryConfiguration {
    private final NbGradleProject project;
    private final Map<DomElementKey, Element> privateConfig;

    public GradleAuxiliaryConfiguration(NbGradleProject project) {
        if (project == null) throw new NullPointerException("project");
        this.project = project;
        this.privateConfig = new ConcurrentHashMap<DomElementKey, Element>();
    }

    private ProjectProperties getProperties() {
        return project.getPropertiesForProfile(null, false, null);
    }

    @Override
    public Element getConfigurationFragment(String elementName, String namespace, boolean shared) {
        DomElementKey key = new DomElementKey(elementName, namespace);
        if (!shared) {
            Element result = privateConfig.get(key);
            return result != null ? (Element)result.cloneNode(true) : null;
        }

        return getProperties().getAuxConfig(elementName, namespace).getProperty().getValue();
    }

    @Override
    public void putConfigurationFragment(Element fragment, boolean shared) throws IllegalArgumentException {
        final String elementName = fragment != null ? fragment.getTagName() : null;
        final String namespace = fragment != null ? fragment.getNamespaceURI() : null;

        final Element newElement = fragment != null
                ? (Element)fragment.cloneNode(true)
                : null;

        DomElementKey key = new DomElementKey(elementName, namespace);
        if (!shared) {
            privateConfig.put(key, newElement);
            return;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getProperties().getAuxConfig(elementName, namespace).getProperty().setValue(newElement);
            }
        });
    }

    @Override
    public boolean removeConfigurationFragment(
            final String elementName,
            final String namespace,
            boolean shared) throws IllegalArgumentException {

        DomElementKey key = new DomElementKey(elementName, namespace);
        if (!shared) {
            return privateConfig.remove(key) != null;
        }

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                getProperties().getAuxConfig(elementName, namespace).getProperty().setValue(null);
            }
        });
        return true;
    }

    public Collection<DomElementKey> getConfigElements(boolean shared) {
        List<DomElementKey> result = new LinkedList<DomElementKey>();
        if (shared) {
            for (AuxConfigProperty property: getProperties().getAllAuxConfigs()) {
                result.add(property.getKey());
            }
        }
        else {
            result.addAll(privateConfig.keySet());
        }
        return result;
    }
}
