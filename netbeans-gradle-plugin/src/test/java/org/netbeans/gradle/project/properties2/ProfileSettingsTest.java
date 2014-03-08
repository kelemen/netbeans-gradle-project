package org.netbeans.gradle.project.properties2;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.OperationCanceledException;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

import static org.junit.Assert.*;

public class ProfileSettingsTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static String getResourcePath(String relPath) {
        return ProfileSettingsTest.class.getPackage().getName().replace('.', '/') + "/" + relPath;
    }

    private static InputStream openResource(String relPath) throws IOException {
        String absolutePath = getResourcePath(relPath);
        ClassLoader classLoader = ProfileSettingsTest.class.getClassLoader();

        URL url = classLoader.getResource(absolutePath);
        if (url == null) {
            throw new IOException("No URL for resource: " + absolutePath);
        }

        InputStream result = classLoader.getResourceAsStream(absolutePath);
        if (result == null) {
            throw new IOException("Failed to open resource: " + absolutePath);
        }
        return result;
    }

    private static void readDocument(ProfileSettings settings, String configFileName) throws IOException {
        try (InputStream input = openResource(configFileName)) {
            settings.loadFromStream(input);
        }
    }

    private static void readFromSettings1(ProfileSettings settings) throws IOException {
        readDocument(settings, "settings1.xml");
    }

    private static ConfigPath getConfigPath(String... keys) {
        List<ConfigKey> configKeys = new ArrayList<>(keys.length);
        for (String key: keys) {
            configKeys.add(new ConfigKey(key, null));
        }
        return ConfigPath.fromKeys(configKeys);
    }

    private static <ValueKey, ValueType> MutableProperty<ValueType> getProperty(
            ProfileSettings settings,
            PropertyDef<ValueKey, ValueType> propertyDef,
            String... keys) {

        ConfigPath configPath = getConfigPath(keys);
        return settings.getProperty(configPath, propertyDef);
    }

    private static void setNodeXmlDef(PropertyDef.Builder<WrappedNodeKey, ?> result) {
        result.setXmlDef(new PropertyXmlDef<WrappedNodeKey>() {
            @Override
            public WrappedNodeKey loadFromXml(Element node) {
                return new WrappedNodeKey((Element)node.cloneNode(true));
            }

            @Override
            public void addToXml(Element parent, WrappedNodeKey value) {
                Document document = parent.getOwnerDocument();

                NodeList children = value.element.getChildNodes();
                int childCount = children.getLength();
                for (int i = 0; i < childCount; i++) {
                    parent.appendChild(document.importNode(children.item(i), true));
                }
            }
        });
    }

    private static PropertyDef<WrappedNodeKey, WrappedNodeValue> getNodeProfileDef() {
        PropertyDef.Builder<WrappedNodeKey, WrappedNodeValue> result = new PropertyDef.Builder<>();
        result.setValueDef(new PropertyValueDef<WrappedNodeKey, WrappedNodeValue>() {
            @Override
            public PropertySource<WrappedNodeValue> property(WrappedNodeKey valueKey) {
                return PropertyFactory.constSource(valueKey != null ? valueKey.toValue() : null);
            }

            @Override
            public WrappedNodeKey getKeyFromValue(WrappedNodeValue value) {
                return value != null ? value.toKey() : null;
            }
        });
        setNodeXmlDef(result);

        return result.create();
    }

    private static PropertyDef<PlatformId, PlatformId> getTargetPlatformProfileDef() {
        PropertyDef.Builder<PlatformId, PlatformId> result = new PropertyDef.Builder<>();
        result.setValueDef(new PropertyValueDef<PlatformId, PlatformId>() {
            @Override
            public PropertySource<PlatformId> property(PlatformId valueKey) {
                return PropertyFactory.constSource(valueKey);
            }

            @Override
            public PlatformId getKeyFromValue(PlatformId value) {
                return value;
            }
        });
        result.setXmlDef(StandardProperties.getTargetPlatformXmlDef());

        return result.create();
    }

    private static PropertyDef<WrappedNodeKey, String> getTextProfileDef() {
        final Document elementFactory;
        try {
            elementFactory = DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }

        PropertyDef.Builder<WrappedNodeKey, String> result = new PropertyDef.Builder<>();
        result.setValueDef(new PropertyValueDef<WrappedNodeKey, String>() {
            @Override
            public PropertySource<String> property(WrappedNodeKey valueKey) {
                return PropertyFactory.constSource(valueKey != null
                        ? valueKey.element.getTextContent()
                        : null);
            }

            @Override
            public WrappedNodeKey getKeyFromValue(String value) {
                if (value == null) {
                    return null;
                }

                Element element = elementFactory.createElement("KEY");
                element.setTextContent(value);

                return new WrappedNodeKey(element);
            }
        });
        setNodeXmlDef(result);

        return result.create();
    }

    private static MutableProperty<String> getTextProperty(
            ProfileSettings settings,
            String... keys) {

        ConfigPath configPath = getConfigPath(keys);
        return settings.getProperty(configPath, getTextProfileDef());
    }

    private static MutableProperty<WrappedNodeValue> getProperty(
            ProfileSettings settings,
            String... keys) {

        ConfigPath configPath = getConfigPath(keys);
        return settings.getProperty(configPath, getNodeProfileDef());
    }

    private static void assertTextNodePropertyValue(
            MutableProperty<WrappedNodeValue> property,
            String expectedValue) {

        WrappedNodeValue value = property.getValue();

        assertNotNull(property + " must have a value.", value);
        assertEquals(property.toString(), expectedValue, value.getText());
    }

    private static void assertTextNodePropertyValue(
            ProfileSettings settings,
            String propertyName,
            String expectedValue) {

        MutableProperty<WrappedNodeValue> property = getProperty(settings, propertyName);
        assertTextNodePropertyValue(property, expectedValue);
    }

    private void testSetValueOfTextProperty(String initialValue, String newValue, String... propertyPath) throws IOException {
        ProfileSettings settings = new ProfileSettings();
        readFromSettings1(settings);

        String propertyName = Arrays.toString(propertyPath);

        MutableProperty<String> property = getTextProperty(settings, propertyPath);
        assertEquals(propertyName, initialValue, property.getValue());

        WaitableListener listener = new WaitableListener();
        property.addChangeListener(listener);

        property.setValue(newValue);
        assertEquals(propertyName, newValue, property.getValue());

        listener.waitForCall();
    }

    @Test
    public void testSetValueOfRootTextProperty() throws IOException {
        testSetValueOfTextProperty("UTF-8", "ISO-8859-1", "source-encoding");
        testSetValueOfTextProperty("j2se", "j2me", "target-platform-name");
        testSetValueOfTextProperty("1.7", "1.7", "target-platform");
        testSetValueOfTextProperty("1.7", "1.7", "source-level");
    }

    @Test
    public void testSetValueOfDeepTextProperty() throws IOException {
        testSetValueOfTextProperty("LF", "CRLF", "auxiliary", "com-junichi11-netbeans-changelf.lf-kind");
    }

    @Test
    public void testMultiNodeProperty() throws IOException {
        ProfileSettings settings = new ProfileSettings();
        readFromSettings1(settings);

        List<ConfigPath> configPaths = Arrays.asList(
                getConfigPath("target-platform-name"),
                getConfigPath("target-platform"));

        MutableProperty<PlatformId> property
                = settings.getProperty(configPaths, getTargetPlatformProfileDef());

        String propertyName = "TargetPlatform";

        assertEquals(propertyName, new PlatformId("j2se", "1.7"), property.getValue());

        WaitableListener documentListener = new WaitableListener();
        settings.addDocumentChangeListener(documentListener);

        WaitableListener listener = new WaitableListener();
        property.addChangeListener(listener);

        PlatformId newValue = new PlatformId("j2me", "1.5");
        property.setValue(newValue);
        assertEquals(propertyName, newValue, property.getValue());

        listener.waitForCall();
        documentListener.waitForCall();
    }

    private static final class WrappedNodeKey {
        public final Element element;

        public WrappedNodeKey(Element element) {
            this.element = element;
        }

        public WrappedNodeValue toValue() {
            return new WrappedNodeValue(element);
        }
    }

    private static final class WrappedNodeValue {
        public final Element element;

        public WrappedNodeValue(Element element) {
            this.element = element;
        }

        public WrappedNodeKey toKey() {
            return new WrappedNodeKey(element);
        }

        public String getText() {
            return element.getTextContent().trim();
        }

        public WrappedNodeValue withText(String newText) {
            Element other = (Element)element.cloneNode(true);
            other.setTextContent(newText);
            return new WrappedNodeValue(other);
        }
    }

    private static final class WaitableListener implements Runnable {
        private final WaitableSignal calledSignal;

        public WaitableListener() {
            this.calledSignal = new WaitableSignal();
        }

        @Override
        public void run() {
            calledSignal.signal();
        }

        public void waitForCall() {
            if (!calledSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, 10, TimeUnit.SECONDS)) {
                throw new OperationCanceledException("timeout");
            }
        }
    }
}
