package org.netbeans.gradle.project.properties;

import java.util.Comparator;
import org.netbeans.gradle.project.api.config.ConfigTree;

public interface ConfigNodeProperty extends Comparator<String> {
    public ConfigNodeProperty getChildSorter(String keyName);

    public boolean ignoreValue();
    public ConfigTree adjustNodes(ConfigTree actualTree);
}
