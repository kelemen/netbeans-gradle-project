package org.netbeans.gradle.project.properties2;

import java.util.Objects;
import javax.annotation.Nullable;
import org.jtrim.utils.ExceptionHelper;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

public final class ConfigKey {
    private final String name;
    private final String namespace;

    public ConfigKey(Node node) {
        this(node.getNodeName(), node.getNamespaceURI());
    }

    public ConfigKey(String name, String namespace) {
        this.name = name;
        this.namespace = namespace;
    }

    public Node tryGetChildNode(Node parent) {
        NodeList childNodes = parent.getChildNodes();
        int length = childNodes.getLength();
        for (int i = 0; i < length; i++) {
            Node child = childNodes.item(i);

            if (Objects.equals(name, child.getNodeName())
                    && Objects.equals(namespace, child.getNamespaceURI())) {
                return child;
            }
        }
        return null;
    }

    public Element addChildIfNeeded(Node parent) {
        ExceptionHelper.checkNotNullArgument(parent, "parent");

        Node childNode = tryGetChildNode(parent);
        if (childNode instanceof Element) {
            return (Element)childNode;
        }

        if (childNode != null) {
            parent.removeChild(childNode);
        }

        Document ownerDocument = Objects.requireNonNull(parent.getOwnerDocument(), "Node needs OwnerDocument");
        Element childElement = ownerDocument.createElementNS(name, namespace);
        // TODO: Allow inserting it in a user defined location.
        parent.appendChild(childElement);
        return childElement;
    }

    @Nullable
    public String getName() {
        return name;
    }

    @Nullable
    public String getNamespace() {
        return namespace;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + Objects.hashCode(name);
        hash = 31 * hash + Objects.hashCode(namespace);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final ConfigKey other = (ConfigKey)obj;

        return Objects.equals(this.name, other.name)
                && Objects.equals(this.namespace, other.namespace);
    }
}
