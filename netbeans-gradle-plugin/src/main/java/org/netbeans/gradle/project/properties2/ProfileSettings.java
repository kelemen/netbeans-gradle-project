package org.netbeans.gradle.project.properties2;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.collections.EqualityComparator;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.CopyOnTriggerListenerManager;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerManager;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySourceProxy;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.xml.sax.SAXException;

public final class ProfileSettings {
    private static final Logger LOGGER = Logger.getLogger(ProfileSettings.class.getName());
    private static final int FILE_STREAM_BUFFER_SIZE = 8 * 1024;
    private static final Set<ConfigPath> ROOT_PATH = Collections.singleton(ConfigPath.ROOT);

    // Must be FIFO
    private static final TaskExecutor EVENT_THREAD
            = SwingTaskExecutor.getStrictExecutor(false);

    // Must be FIFO / ProfileSettings instance.
    private static final MonitorableTaskExecutorService DOCUMENT_ACCESSOR_THREAD
            = NbTaskExecutors.newExecutor("Property-Updater", 1);

    private final ListenerManager<DocumentUpdateListener> documentUpdateListeners;
    private final EventDispatcher<DocumentUpdateListener, Collection<ConfigPath>> documentUpdateDispatcher;

    private Document currentDocument;
    private volatile boolean loadedDocumentAtLeastOnce;

    public ProfileSettings() {
        this.documentUpdateListeners = new CopyOnTriggerListenerManager<>();
        this.currentDocument = getEmptyDocument();
        this.loadedDocumentAtLeastOnce = false;

        this.documentUpdateDispatcher = new EventDispatcher<DocumentUpdateListener, Collection<ConfigPath>>() {
            @Override
            public void onEvent(DocumentUpdateListener eventListener, Collection<ConfigPath> arg) {
                eventListener.updateDocument(arg);
            }
        };
    }

    public static boolean isEventThread() {
        return SwingUtilities.isEventDispatchThread();
    }

    ListenerRef addDocumentChangeListener(final Runnable listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        return documentUpdateListeners.registerListener(new DocumentUpdateListener() {
            @Override
            public void updateDocument(Collection<ConfigPath> changedPaths) {
                listener.run();
            }
        });
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
        ExceptionHelper.checkNotNullArgument(xmlSource, "xmlSource");

        return getDocumentBuilder().parse(xmlSource);
    }

    private static Document readXml(Path xmlFile) throws IOException, SAXException {
        ExceptionHelper.checkNotNullArgument(xmlFile, "xmlFile");

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

    private void fireDocumentUpdate(final Collection<ConfigPath> path) {
        DOCUMENT_ACCESSOR_THREAD.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) {
                documentUpdateListeners.onEvent(documentUpdateDispatcher, path);
            }
        }, null);
    }

    private void loadFromDocument(final Document document) {
        ExceptionHelper.checkNotNullArgument(document, "document");

        loadedDocumentAtLeastOnce = true;
        DOCUMENT_ACCESSOR_THREAD.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                currentDocument = document;
                fireDocumentUpdate(ROOT_PATH);
            }
        }, null);
    }

    private Document getDocument() {
        assert DOCUMENT_ACCESSOR_THREAD.isExecutingInThis();
        return currentDocument;
    }

    public <ValueKey, ValueType> MutableProperty<ValueType> getProperty(
            ConfigPath configPath,
            PropertyDef<ValueKey, ValueType> propertyDef) {
        return getProperty(Collections.singleton(configPath), propertyDef);
    }

    public <ValueKey, ValueType> MutableProperty<ValueType> getProperty(
            Collection<ConfigPath> configPaths,
            PropertyDef<ValueKey, ValueType> propertyDef) {
        ExceptionHelper.checkNotNullArgument(configPaths, "configPaths");
        ExceptionHelper.checkNotNullArgument(propertyDef, "propertyDef");

        DomTrackingProperty<ValueKey, ValueType> result
                = new DomTrackingProperty<>(configPaths, propertyDef);
        result.init(Cancellation.UNCANCELABLE_TOKEN);
        return result;
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

        List<ConfigKey> result = new LinkedList<>();

        outerLoop:
        for (int keyIndex = 0; keyIndex < minLength; keyIndex++) {
            ConfigKey key = paths[0].getKeyAt(keyIndex);
            for (int pathIndex = 1; pathIndex < paths.length; pathIndex++) {
                if (!key.equals(paths[pathIndex].getKeyAt(keyIndex))) {
                    break outerLoop;
                }
            }
            result.add(key);
        }

        return ConfigPath.fromKeys(result);
    }

    private static interface DocumentUpdateListener {
        public void updateDocument(Collection<ConfigPath> changedPaths);
    }

    private class DomTrackingProperty<ValueKey, ValueType>
    implements
            MutableProperty<ValueType> {

        private final ConfigPath configParent;
        private final ConfigPath[] configPaths;
        private final List<ConfigPath> configPathsAsList;

        private final PropertyXmlDef<ValueKey> xmlDef;
        private final PropertyValueDef<ValueKey, ValueType> valueDef;
        private final EqualityComparator<? super ValueKey> valueKeyEquality;

        private final UpdateTaskExecutor valueUpdaterThread;
        private final UpdateTaskExecutor eventThread;

        private final PropertySourceProxy<ValueType> source;

        private boolean lastValueKeyInitialized;
        private ValueKey lastValueKey;

        public DomTrackingProperty(
                Collection<ConfigPath> configPaths,
                PropertyDef<ValueKey, ValueType> propertyDef) {

            this.configPathsAsList = copyPaths(configPaths);
            this.configPaths = configPathsAsList.toArray(new ConfigPath[configPathsAsList.size()]);
            this.configParent = getCommonParent(this.configPaths);

            this.xmlDef = propertyDef.getXmlDef();
            this.valueDef = propertyDef.getValueDef();
            this.valueKeyEquality = propertyDef.getValueKeyEquality();

            this.source = PropertyFactory.proxySource(valueDef.property(null));

            this.valueUpdaterThread = new GenericUpdateTaskExecutor(DOCUMENT_ACCESSOR_THREAD);
            this.eventThread = new GenericUpdateTaskExecutor(EVENT_THREAD);

            this.lastValueKey = null;
            this.lastValueKeyInitialized = false;

            ExceptionHelper.checkNotNullElements(this.configPaths, "configPaths");
        }

        public void init(CancellationToken cancelToken) {
            if (!loadedDocumentAtLeastOnce) {
                // In this case the client code should not expect that the
                // property value is up to date.
                valueUpdaterThread.execute(new Runnable() {
                    @Override
                    public void run() {
                        updateFromDocument();
                    }
                });
                return;
            }

            if (DOCUMENT_ACCESSOR_THREAD.isExecutingInThis()) {
                updateFromDocument();
            }
            else {
                DOCUMENT_ACCESSOR_THREAD.submit(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
                    @Override
                    public void execute(CancellationToken cancelToken) {
                        updateFromDocument();
                    }
                }, null).waitAndGet(cancelToken);
            }
        }

        private void updateDocumentFromKey(ValueKey valueKey) {
            assert DOCUMENT_ACCESSOR_THREAD.isExecutingInThis();

            if (lastValueKeyInitialized && valueKeyEquality.equals(valueKey, lastValueKey)) {
                return;
            }

            lastValueKey = valueKey;
            lastValueKeyInitialized = true;

            Element root = getDocument().getDocumentElement();
            if (root != null) {
                for (ConfigPath path: configPaths) {
                    path.removeFromNode(root);
                }
            }

            if (valueKey != null) {
                for (ConfigPath path: configPaths) {
                    xmlDef.addToXml(path.addToNode(root), valueKey);
                }
            }

            fireDocumentUpdate(configPathsAsList);
        }

        @Override
        public void setValue(final ValueType value) {
            final ValueKey valueKey = valueDef.getKeyFromValue(value);
            source.replaceSource(valueDef.property(valueKey));

            valueUpdaterThread.execute(new Runnable() {
                @Override
                public void run() {
                    updateDocumentFromKey(valueKey);
                }
            });
        }

        @Override
        public ValueType getValue() {
            return source.getValue();
        }

        private boolean affectsThis(Collection<ConfigPath> changedPaths) {
            if (changedPaths == configPathsAsList) {
                // This event is comming from us, so we won't update.
                // This check is not necessary for correctness but to avoid
                // unecessary reparsing of the property value.
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

        private ValueKey getValueKey() {
            assert DOCUMENT_ACCESSOR_THREAD.isExecutingInThis();

            Element root = getDocument().getDocumentElement();
            if (root == null) {
                return null;
            }

            Element child = configParent.tryGetChildElement(root);
            if (child == null) {
                return null;
            }

            return xmlDef.loadFromXml(child);
        }

        private void updateFromDocument() {
            assert DOCUMENT_ACCESSOR_THREAD.isExecutingInThis();

            source.replaceSource(valueDef.property(getValueKey()));
        }

        @Override
        public ListenerRef addChangeListener(final Runnable listener) {
            ExceptionHelper.checkNotNullArgument(listener, "listener");

            ListenerRef ref1 = documentUpdateListeners.registerListener(new DocumentUpdateListener() {
                @Override
                public void updateDocument(Collection<ConfigPath> changedPaths) {
                    if (affectsThis(changedPaths)) {
                        updateFromDocument();
                    }
                }
            });

            ListenerRef ref2 = source.addChangeListener(new Runnable() {
                @Override
                public void run() {
                    eventThread.execute(listener);
                }
            });

            return ListenerRegistries.combineListenerRefs(ref1, ref2);
        }

        @Override
        public String toString() {
            return "Property{" + configPaths + '}';
        }
    }
}
