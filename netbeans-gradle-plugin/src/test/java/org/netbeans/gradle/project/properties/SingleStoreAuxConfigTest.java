package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.swing.SwingUtilities;
import org.junit.Before;
import org.junit.Test;
import org.w3c.dom.Element;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class SingleStoreAuxConfigTest {
    private TestAuxConfigStorage storage;
    private SingleStoreAuxConfig auxConfig;

    @Before
    public void setUp() {
        storage = new TestAuxConfigStorage();
        auxConfig = new SingleStoreAuxConfig(storage);
    }

    private static AuxConfigProperty createAuxProperty(DomElementKey key, Element initialValue) {
        return new AuxConfigProperty(
                key, new DefaultMutableProperty<>(initialValue, false, true));
    }

    private static AuxConfigProperty createAuxProperty(DomElementKey key) {
        return createAuxProperty(key, null);
    }

    private void addToStorage(DomElementKey key, Element value) {
        AuxConfigProperty property = createAuxProperty(key, value);
        storage.addProperty(key, property);
    }

    private static Element createFromKey(DomElementKey key) {
        Element value = mock(Element.class);
        stub(value.getTagName()).toReturn(key.getName());
        stub(value.getNamespaceURI()).toReturn(key.getNamespace());
        stub(value.cloneNode(anyBoolean())).toReturn(value);
        return value;
    }

    @Test
    public void testGetConfigurationFragmentRightAfterSet() throws Exception {
        final DomElementKey key = new DomElementKey("name", "namespace");
        final Element value = createFromKey(key);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                auxConfig.putConfigurationFragment(value);
                Element received = auxConfig.getConfigurationFragment(key.getName(), key.getNamespace());
                assertSame(value, received);
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                storage.verifyValue(key, value);
            }
        });
    }

    @Test
    public void testRemoveConfigurationFragmentRightAfterSet() throws Exception {
        final DomElementKey key = new DomElementKey("name", "namespace");
        final Element value = createFromKey(key);

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                auxConfig.putConfigurationFragment(value);
                auxConfig.removeConfigurationFragment(key.getName(), key.getNamespace());
                Element received = auxConfig.getConfigurationFragment(key.getName(), key.getNamespace());
                assertNull(received);
            }
        });

        SwingUtilities.invokeAndWait(new Runnable() {
            @Override
            public void run() {
                storage.verifyNotSet(key);
            }
        });
    }

    @Test
    public void testGetConfigElements() {
        final DomElementKey key1 = new DomElementKey("name", "namespace");
        final Element value1 = createFromKey(key1);

        final DomElementKey key2 = new DomElementKey("nameX", "namespace");
        final Element value2 = createFromKey(key2);

        final DomElementKey key3 = new DomElementKey("name", "namespaceX");
        final Element value3 = createFromKey(key3);

        final Element valuex = createFromKey(key1);

        addToStorage(key1, value1);
        addToStorage(key2, value2);
        addToStorage(key3, value3);
        addToStorage(key1, valuex);

        Set<DomElementKey> expected = new HashSet<>();
        expected.add(key1);
        expected.add(key2);
        expected.add(key3);

        Collection<DomElementKey> elements = auxConfig.getConfigElements();

        HashSet<DomElementKey> elementsSet = new HashSet<>(elements);
        assertEquals("Multiple keys are not allowed.", expected.size(), elementsSet.size());
        assertEquals(expected, elementsSet);
    }

    private static final class TestAuxConfigStorage implements SingleStoreAuxConfig.AuxConfigStorage {
        private final ConcurrentMap<DomElementKey, AuxConfigProperty> properties;

        public TestAuxConfigStorage() {
            this.properties = new ConcurrentHashMap<>();
        }

        public void addProperty(DomElementKey key, AuxConfigProperty value) {
            properties.put(key, value);
        }

        public void verifyNotSet(DomElementKey key) {
            AuxConfigProperty value = properties.get(key);
            if (value != null) {
                assertNull(value.getProperty().getValue());
            }
        }

        public void verifyValue(DomElementKey key, Element expectedValue) {
            AuxConfigProperty value = properties.get(key);
            assertNotNull(value);
            assertSame(expectedValue, value.getProperty().getValue());
        }

        @Override
        public AuxConfigProperty getAuxConfig(String elementName, String namespace) {
            DomElementKey key = new DomElementKey(elementName, namespace);
            AuxConfigProperty result = properties.get(key);
            if (result == null) {
                properties.putIfAbsent(key, createAuxProperty(key));
                result = properties.get(key);
            }
            return result;
        }

        @Override
        public Collection<AuxConfigProperty> getAllAuxConfigs() {
            return new ArrayList<>(properties.values());
        }
    }
}
