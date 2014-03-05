package org.netbeans.gradle.project.properties2;

import java.util.Objects;
import javax.annotation.Nullable;
import org.w3c.dom.Node;

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
