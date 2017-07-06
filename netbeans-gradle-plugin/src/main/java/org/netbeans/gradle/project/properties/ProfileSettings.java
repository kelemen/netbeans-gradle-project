package org.netbeans.gradle.project.properties;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.ReentrantLock;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.annotation.Nullable;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jtrim2.collections.EqualityComparator;
import org.jtrim2.event.CopyOnTriggerListenerManager;
import org.jtrim2.event.ListenerManager;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySourceProxy;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

public final class ProfileSettings {
    private static final Logger LOGGER = Logger.getLogger(ProfileSettings.class.getName());
    private static final int FILE_STREAM_BUFFER_SIZE = 8 * 1024;
    private static final Set<ConfigPath> ROOT_PATH = Collections.singleton(ConfigPath.ROOT);
    private static final Document EXPORT_DOCUMENT = tryCreateDocument();

    private final ListenerManager<ConfigUpdateListener> configUpdateListeners;

    private final ReentrantLock configLock;
    private volatile Object configStateKey;
    private ConfigTree.Builder currentConfig;
    private final Map<DomElementKey, Element> auxConfigs;

    private static Document tryCreateDocument() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder().newDocument();
        } catch (ParserConfigurationException ex) {
            LOGGER.log(Level.SEVERE, "Cannot create document.", ex);
            return null;
        }
    }

    public ProfileSettings() {
        this.configLock = new ReentrantLock();
        this.currentConfig = new ConfigTree.Builder();
        this.configUpdateListeners = new CopyOnTriggerListenerManager<>();
        this.configStateKey = new Object();
        this.auxConfigs = new HashMap<>();
    }

    public static boolean isEventThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    ListenerRef addDocumentChangeListener(Runnable listener) {
        Objects.requireNonNull(listener, "listener");
        return configUpdateListeners.registerListener(changedPaths -> listener.run());
    }

    private static DocumentBuilder getDocumentBuilder() {
        try {
            return DocumentBuilderFactory.newInstance().newDocumentBuilder();
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException("Cannot create Document builder.", ex);
        }
    }

    private static Document getEmptyDocument() {
        return getDocumentBuilder().newDocument();
    }

    private static Document readXml(InputStream xmlSource) throws IOException, SAXException {
        Objects.requireNonNull(xmlSource, "xmlSource");
        return getDocumentBuilder().parse(xmlSource);
    }

    private static Document readXml(Path xmlFile) throws IOException, SAXException {
        Objects.requireNonNull(xmlFile, "xmlFile");

        if (!Files.exists(xmlFile)) {
            return getEmptyDocument();
        }

        try (InputStream fileInput = Files.newInputStream(xmlFile);
                InputStream input = new BufferedInputStream(fileInput, FILE_STREAM_BUFFER_SIZE)) {
            return readXml(input);
        }
    }

    public void loadFromFile(Path xmlFile) {
        Document document;
        try {
            document = readXml(xmlFile);
        } catch (IOException | SAXException ex) {
            LOGGER.log(Level.INFO, "Unable to parse XML config file: " + xmlFile, ex);
            return;
        }

        loadFromDocument(document);
    }

    public void loadFromStream(InputStream xmlSource) {
        Document document;
        try {
            document = readXml(xmlSource);
        } catch (IOException | SAXException ex) {
            LOGGER.log(Level.INFO, "Unable to parse XML config file from stream.", ex);
            return;
        }

        loadFromDocument(document);
    }

    public ConfigTree getContentSnapshot() {
        return ConfigXmlUtils.parseDocument(toXml()).create();
    }

    private Document toXml() {
        ConfigTree configTree;
        List<Element> auxConfigList;

        configLock.lock();
        try {
            configTree = currentConfig.create();
            auxConfigList = new ArrayList<>(auxConfigs.values());
        } finally {
            configLock.unlock();
        }

        Document document;
        try {
            document = ConfigXmlUtils.createXml(configTree);
        } catch (ParserConfigurationException ex) {
            throw new RuntimeException(ex);
        }

        ConfigXmlUtils.addAuxiliary(document, auxConfigList.toArray(new Element[auxConfigList.size()]));

        return document;
    }

    public void saveToFile(Path xmlFile, ConfigSaveOptions saveOptions) throws IOException {
        Objects.requireNonNull(xmlFile, "xmlFile");
        Objects.requireNonNull(saveOptions, "saveOptions");

        Document document = toXml();

        Path outputDir = xmlFile.getParent();
        if (outputDir != null) {
            Files.createDirectories(outputDir);
        }

        ConfigXmlUtils.saveXmlTo(document, xmlFile, saveOptions);
    }

    private void fireDocumentUpdate(Collection<ConfigPath> path) {
        configUpdateListeners.onEvent(ConfigUpdateListener::configUpdated, path);
    }

    private Object newConfigState() {
        assert configLock.isHeldByCurrentThread();

        Object newState = new Object();
        configStateKey = newState;
        return newState;
    }

    private static Node getChildByName(Element parent, String childName) {
        NodeList children = parent.getChildNodes();
        int childCount = children.getLength();
        for (int i = 0; i < childCount; i++) {
            Node child = children.item(i);
            if (Objects.equals(child.getNodeName(), childName)) {
                return child;
            }
        }
        return null;
    }

    private static List<Element> getAuxiliaryElements(Element parent) {
        if (parent == null) {
            return Collections.emptyList();
        }

        Node auxRoot = getChildByName(parent, ConfigXmlUtils.AUXILIARY_NODE_NAME);
        if (auxRoot == null) {
            return Collections.emptyList();
        }

        NodeList children = auxRoot.getChildNodes();
        int childCount = children.getLength();
        List<Element> result = new ArrayList<>(childCount);

        for (int i = 0; i < childCount; i++) {
            Node child = children.item(i);
            if (child instanceof Element) {
                result.add((Element)child);
            }
        }
        return result;
    }

    public void clearSettings() {
        configLock.lock();
        try {
            auxConfigs.clear();
            currentConfig = new ConfigTree.Builder();
            newConfigState();
        } finally {
            configLock.unlock();
        }

        fireDocumentUpdate(ROOT_PATH);
    }

    private void loadFromDocument(Document document) {
        Objects.requireNonNull(document, "document");

        ConfigTree.Builder parsedDocument = ConfigXmlUtils.parseDocument(document, ConfigXmlUtils.AUXILIARY_NODE_NAME);
        List<Element> loadedAuxConfigs = getAuxiliaryElements(document.getDocumentElement());

        configLock.lock();
        try {
            auxConfigs.clear();
            for (Element entry: loadedAuxConfigs) {
                auxConfigs.put(new DomElementKey(entry.getNodeName(), entry.getNamespaceURI()), entry);
            }

            currentConfig = parsedDocument;
            newConfigState();
        } finally {
            configLock.unlock();
        }

        fireDocumentUpdate(ROOT_PATH);
    }

    private static ConfigTree createChildTree(ConfigTree.Builder builer, ConfigPath path) {
        ConfigTree.Builder childBuilder = builer.getDeepChildBuilder(path);
        childBuilder.detachChildTreeBuilders();
        return childBuilder.create();
    }

    private <Value> ValueWithStateKey<Value> withStateKey(Value value) {
        assert configLock.isHeldByCurrentThread();
        return new ValueWithStateKey<>(configStateKey, value);
    }

    private ValueWithStateKey<ConfigTree> getChildConfig(ConfigPath path) {
        configLock.lock();
        try {
            return withStateKey(createChildTree(currentConfig, path));
        } finally {
            configLock.unlock();
        }
    }

    private ValueWithStateKey<ConfigTree> getChildConfig(ConfigPath basePath, ConfigPath[] relPaths) {
        if (relPaths.length == 1) {
            assert relPaths[0].getKeyCount() == 0;

            // Common case
            return getChildConfig(basePath);
        }

        Object resultStateKey;
        ConfigTree.Builder result = new ConfigTree.Builder();
        configLock.lock();
        try {
            resultStateKey = configStateKey;

            ConfigTree.Builder baseBuilder = currentConfig.getDeepChildBuilder(basePath);
            for (ConfigPath relPath: relPaths) {
                ConfigTree childTree = createChildTree(baseBuilder, relPath);
                setChildTree(result, relPath, childTree);
            }
        } finally {
            configLock.unlock();
        }

        return new ValueWithStateKey<>(resultStateKey, result.create());
    }

    public Collection<DomElementKey> getAuxConfigKeys() {
        configLock.lock();
        try {
            return new ArrayList<>(auxConfigs.keySet());
        } finally {
            configLock.unlock();
        }
    }

    public Element getAuxConfigValue(DomElementKey key) {
        Objects.requireNonNull(key, "key");

        Element result;
        configLock.lock();
        try {
            result = auxConfigs.get(key);
        } finally {
            configLock.unlock();
        }

        return result != null
                ? (Element)EXPORT_DOCUMENT.importNode(result, true)
                : null;
    }

    public boolean setAuxConfigValue(DomElementKey key, Element value) {
        Objects.requireNonNull(key, "key");

        Element toAdd = value != null
                ? (Element)EXPORT_DOCUMENT.importNode(value, true)
                : null;

        configLock.lock();
        try {
            if (toAdd == null) {
                return auxConfigs.remove(key) != null;
            }
            else {
                auxConfigs.put(key, value);
                return true;
            }
        } finally {
            configLock.unlock();
        }
    }

    public <ValueType> MutableProperty<ValueType> getProperty(
            PropertyDef<?, ValueType> propertyDef) {
        return new DomTrackingProperty<>(propertyDef);
    }

    private static List<ConfigPath> copyPaths(Collection<ConfigPath> paths) {
        switch (paths.size()) {
            case 0:
                return Collections.emptyList();
            case 1:
                return Collections.singletonList(paths.iterator().next());
            default:
                return Collections.unmodifiableList(new ArrayList<>(paths));
        }
    }

    private static ConfigPath[] removeTopParents(int removeCount, ConfigPath[] paths) {
        if (removeCount == 0) {
            return paths;
        }

        ConfigPath[] result = new ConfigPath[paths.length];
        for (int i = 0; i < result.length; i++) {
            List<String> keys = paths[i].getKeys();
            result[i] = ConfigPath.fromKeys(keys.subList(removeCount, keys.size()));
        }
        return result;
    }

    private static ConfigPath getCommonParent(ConfigPath[] paths) {
        if (paths.length == 1) {
            // Almost every time this path is taken.
            return paths[0];
        }
        if (paths.length == 0) {
            return ConfigPath.ROOT;
        }

        int minLength = paths[0].getKeyCount();
        for (int i = 1; i < paths.length; i++) {
            int keyCount = paths[i].getKeyCount();
            if (keyCount < minLength) minLength = keyCount;
        }

        List<String> result = new ArrayList<>();

        outerLoop:
        for (int keyIndex = 0; keyIndex < minLength; keyIndex++) {
            String key = paths[0].getKeyAt(keyIndex);
            for (int pathIndex = 1; pathIndex < paths.length; pathIndex++) {
                if (!key.equals(paths[pathIndex].getKeyAt(keyIndex))) {
                    break outerLoop;
                }
            }
            result.add(key);
        }

        return ConfigPath.fromKeys(result);
    }

    private static void setChildTree(ConfigTree.Builder builder, ConfigPath path, ConfigTree content) {
        int keyCount = path.getKeyCount();
        assert keyCount > 0;

        ConfigTree.Builder childConfig = builder;
        for (int i = 0; i < keyCount - 1; i++) {
            childConfig = childConfig.getChildBuilder(path.getKeyAt(i));
        }
        childConfig.setChildTree(path.getKeyAt(keyCount - 1), content);
    }

    private <ValueKey> ValueWithStateKey<ValueKey> getValueKeyFromCurrentConfig(
            ConfigPath parent,
            ConfigPath[] relativePaths,
            PropertyKeyEncodingDef<ValueKey> keyEncodingDef) {

        ValueWithStateKey<ConfigTree> parentBasedConfig = getChildConfig(parent, relativePaths);
        ConfigTree value = parentBasedConfig.value;
        assert value != null;

        return parentBasedConfig.withNewValue(keyEncodingDef.decode(value));
    }

    private static interface ConfigUpdateListener {
        public void configUpdated(Collection<ConfigPath> changedPaths);
    }

    private class DomTrackingProperty<ValueKey, ValueType>
    implements
            MutableProperty<ValueType> {

        private final ConfigPath configParent;
        private final ConfigPath[] configPaths;
        private final ConfigPath[] relativeConfigPaths;
        private final List<ConfigPath> configPathsAsList;

        private final PropertyKeyEncodingDef<ValueKey> keyEncodingDef;
        private final PropertyValueDef<ValueKey, ValueType> valueDef;
        private final EqualityComparator<? super ValueKey> valueKeyEquality;
        private final AtomicReference<ValueWithStateKey<ValueKey>> lastValueKeyRef;

        private final UpdateTaskExecutor eventThread;

        private final PropertySourceProxy<ValueType> source;

        public DomTrackingProperty(PropertyDef<ValueKey, ValueType> propertyDef) {
            Objects.requireNonNull(propertyDef, "propertyDef");

            this.configPathsAsList = copyPaths(propertyDef.getConfigPaths());
            this.configPaths = configPathsAsList.toArray(new ConfigPath[configPathsAsList.size()]);
            this.configParent = getCommonParent(this.configPaths);
            this.relativeConfigPaths = removeTopParents(configParent.getKeyCount(), this.configPaths);

            this.keyEncodingDef = propertyDef.getKeyEncodingDef();
            this.valueDef = propertyDef.getValueDef();
            this.valueKeyEquality = propertyDef.getValueKeyEquality();

            ValueWithStateKey<ValueKey> initialValueKey = getValueKeyFromCurrentConfig(
                    this.configParent,
                    this.relativeConfigPaths,
                    this.keyEncodingDef);
            this.lastValueKeyRef = new AtomicReference<>(initialValueKey);
            this.source = PropertyFactory.proxySource(valueDef.property(initialValueKey.value));

            this.eventThread = SwingExecutors.getSwingUpdateExecutor(true);
        }

        private void updateConfigFromKey() {
            ValueWithStateKey<ValueKey> valueKey;
            ValueWithStateKey<ValueKey> newValueKey;

            do {
                valueKey = lastValueKeyRef.get();
                newValueKey = updateConfigFromKey(valueKey);
            } while (!lastValueKeyRef.compareAndSet(valueKey, newValueKey));
        }

        private ValueWithStateKey<ValueKey> updateConfigFromKey(ValueWithStateKey<ValueKey> valueKeyWithState) {
            // Should only be called by updateConfigFromKey()

            ValueKey valueKey = valueKeyWithState.value;
            ConfigTree encodedValueKey = valueKey != null ? keyEncodingDef.encode(valueKey) : ConfigTree.EMPTY;
            Object newState;

            configLock.lock();
            try {
                int pathCount = relativeConfigPaths.length;
                for (int i = 0; i < pathCount; i++) {
                    ConfigPath relativePath = relativeConfigPaths[i];
                    ConfigPath path = configPaths[i];

                    ConfigTree configTree = encodedValueKey.getDeepChildTree(relativePath);
                    updateConfigAtPath(path, configTree);
                }

                newState = newConfigState();
            } finally {
                configLock.unlock();
            }

            fireDocumentUpdate(configPathsAsList);
            return new ValueWithStateKey<>(newState, valueKeyWithState.value);
        }

        private void updateConfigAtPath(ConfigPath path, ConfigTree content) {
            assert configLock.isHeldByCurrentThread();

            if (path.getKeyCount() == 0) {
                currentConfig = new ConfigTree.Builder(content);
            }
            else {
                setChildTree(currentConfig, path, content);
            }
        }

        private ValueWithStateKey<ValueKey> getUpToDateValueKey() {
            ValueWithStateKey<ValueKey> lastValueKey;
            Object currentConfigStateKey;

            while (true) {
                lastValueKey = lastValueKeyRef.get();
                currentConfigStateKey = configStateKey;

                if (currentConfigStateKey == lastValueKey.stateKey) {
                    // It is possible that there was a concurrent configuration
                    // reload but in this case we can't decide if it came before
                    // us or not, so we conveniently declare ourselves as the winner.

                    return lastValueKey;
                }
                else {
                    updateFromConfig();
                }
            }
        }

        @Override
        public void setValue(final ValueType value) {
            ValueWithStateKey<ValueKey> lastValueKey = getUpToDateValueKey();

            ValueKey valueKey = valueDef.getKeyFromValue(value);
            if (updateSource(lastValueKey.withNewValue(valueKey))) {
                updateConfigFromKey();
            }
        }

        @Override
        public ValueType getValue() {
            if (lastValueKeyRef.get().stateKey != configStateKey) {
                updateFromConfig();
            }

            return source.getValue();
        }

        private boolean affectsThis(Collection<ConfigPath> changedPaths) {
            if (changedPaths == configPathsAsList) {
                // This event is comming from us, so we won't update.
                // This is necessary for correctness to avoid infinite loop
                // in updateConfigFromKey()
                return false;
            }

            for (ConfigPath changedPath: changedPaths) {
                for (ConfigPath ourPath: configPaths) {
                    if (changedPath.isParentOfOrEqual(ourPath)) {
                        return true;
                    }
                }
            }
            return false;
        }

        private ValueWithStateKey<ValueKey> getValueKey() {
            return getValueKeyFromCurrentConfig(configParent, relativeConfigPaths, keyEncodingDef);
        }

        private boolean updateSource(ValueWithStateKey<ValueKey> valueKey) {
            ValueWithStateKey<ValueKey> prevValueKey = lastValueKeyRef.getAndSet(valueKey);
            if (valueKeyEquality.equals(prevValueKey.value, valueKey.value)) {
                return false;
            }
            else {
                source.replaceSource(valueDef.property(valueKey.value));
                return true;
            }
        }

        private void updateFromConfig() {
            updateSource(getValueKey());
        }

        @Override
        public ListenerRef addChangeListener(Runnable listener) {
            Objects.requireNonNull(listener, "listener");

            ListenerRef ref1 = configUpdateListeners.registerListener((Collection<ConfigPath> changedPaths) -> {
                if (affectsThis(changedPaths)) {
                    updateFromConfig();
                }
            });

            ListenerRef ref2 = source.addChangeListener(() -> eventThread.execute(listener));

            return ListenerRefs.combineListenerRefs(ref1, ref2);
        }

        @Override
        public String toString() {
            return "Property{" + Arrays.toString(configPaths) + '}';
        }
    }

    private static final class ValueWithStateKey<Value> {
        public final Object stateKey;

        @Nullable
        public final Value value;

        public ValueWithStateKey(Object stateKey, Value valueKey) {
            this.stateKey = stateKey;
            this.value = valueKey;
        }

        public <NewValue> ValueWithStateKey<NewValue> withNewValue(NewValue newValue) {
            return new ValueWithStateKey<>(stateKey, newValue);
        }
    }
}
