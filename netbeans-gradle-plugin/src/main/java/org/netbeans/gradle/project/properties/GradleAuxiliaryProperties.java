package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

public final class GradleAuxiliaryProperties implements AuxiliaryProperties {
    private final GradleAuxiliaryConfiguration config;
    private final Document elementFactory;
    private final Lock factoryLock;

    public GradleAuxiliaryProperties(GradleAuxiliaryConfiguration config) {
        this.config = Objects.requireNonNull(config, "config");
        this.factoryLock = new ReentrantLock();
        try {
            this.elementFactory = DocumentBuilderFactory
                    .newInstance()
                    .newDocumentBuilder()
                    .newDocument();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex.getMessage(), ex);
        }
    }

    private Element createElement(String name) {
        factoryLock.lock();
        try {
            return elementFactory.createElement(name);
        } finally {
            factoryLock.unlock();
        }
    }

    @Override
    public String get(String key, boolean shared) {
        Element configNode = config.getConfigurationFragment(key, null, shared);
        return configNode != null ? configNode.getTextContent() : null;
    }

    @Override
    public void put(String key, String value, boolean shared) {
        if (value == null) {
            config.removeConfigurationFragment(key, null, shared);
            return;
        }

        Element configNode = createElement(key);
        configNode.setTextContent(value);

        config.putConfigurationFragment(configNode, shared);
    }

    @Override
    public Iterable<String> listKeys(boolean shared) {
        List<String> result = new ArrayList<>();
        for (DomElementKey key: config.getConfigElements(shared)) {
            if (key.getNamespace() == null) {
                result.add(key.getName());
            }
        }
        return result;
    }
}
