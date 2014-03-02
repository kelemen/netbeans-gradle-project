package org.netbeans.gradle.project.properties;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import javax.swing.SwingUtilities;
import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Element;

public final class SingleStoreAuxConfig {
    private final AuxConfigStorage storage;
    private final ConcurrentMap<DomElementKey, ElementRef> temporaryStore;

    public SingleStoreAuxConfig(AuxConfigStorage storage) {
        ExceptionHelper.checkNotNullArgument(storage, "storage");
        this.storage = storage;
        this.temporaryStore = new ConcurrentHashMap<>();
    }

    public Element getConfigurationFragment(String elementName, String namespace) {
        DomElementKey key = new DomElementKey(elementName, namespace);
        Element result = storage.getAuxConfig(elementName, namespace).getProperty().getValue();

        ElementRef tmpResultRef = temporaryStore.get(key);
        if (tmpResultRef != null) {
            return tmpResultRef.element;
        }
        else {
            return result;
        }
    }

    public void putConfigurationFragment(Element fragment) {
        final String elementName = fragment != null ? fragment.getTagName() : null;
        final String namespace = fragment != null ? fragment.getNamespaceURI() : null;

        Element newElement = fragment != null
                ? (Element)fragment.cloneNode(true)
                : null;

        final DomElementKey key = new DomElementKey(elementName, namespace);
        final ElementRef newElementRef = new ElementRef(newElement);

        temporaryStore.put(key, newElementRef);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                storage.getAuxConfig(elementName, namespace).getProperty().setValue(newElementRef.element);
                temporaryStore.remove(key, newElementRef);
            }
        });
    }

    public boolean removeConfigurationFragment(final String elementName, final String namespace) {
        DomElementKey key = new DomElementKey(elementName, namespace);
        temporaryStore.remove(key);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                storage.getAuxConfig(elementName, namespace).getProperty().setValue(null);
            }
        });
        return true;
    }

    public Collection<DomElementKey> getConfigElements() {
        Set<DomElementKey> result = new LinkedHashSet<>();
        for (AuxConfigProperty property: storage.getAllAuxConfigs()) {
            result.add(property.getKey());
        }

        // Use manual copy, because addAll has no defined behaviour if
        // the keyset changes concurrently. Think of the
        // toArray(new Object[collections.size()]) idiom.
        for (DomElementKey key: temporaryStore.keySet()) {
            result.add(key);
        }
        return result;
    }

    public static interface AuxConfigStorage {
        @Nonnull
        public AuxConfigProperty getAuxConfig(@Nullable String elementName, @Nullable String namespace);

        @Nonnull
        public Collection<AuxConfigProperty> getAllAuxConfigs();
    }

    // This class is used to ensure identity comparison for Element instances,
    // and to allow null values.
    private static final class ElementRef {
        public final Element element;

        public ElementRef(Element element) {
            this.element = element;
        }
    }
}
