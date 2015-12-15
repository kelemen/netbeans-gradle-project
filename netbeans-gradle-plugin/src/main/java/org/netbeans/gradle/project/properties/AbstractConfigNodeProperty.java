package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.api.config.ConfigTree;

public abstract class AbstractConfigNodeProperty implements ConfigNodeProperty {
    @Override
    public ConfigNodeProperty getChildSorter(String keyName) {
        return DefaultConfigNodeProperty.INSTANCE;
    }

    @Override
    public int compare(String o1, String o2) {
        return o1.compareTo(o2);
    }

    @Override
    public boolean ignoreValue() {
        return false;
    }

    @Override
    public ConfigTree adjustNodes(ConfigTree actualTree) {
        return actualTree;
    }
}
