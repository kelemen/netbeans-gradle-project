package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.api.config.ConfigTree;
import java.util.Comparator;

public interface ConfigNodeProperty extends Comparator<String> {
    public ConfigNodeProperty getChildSorter(String keyName);

    public boolean ignoreValue();
    public ConfigTree adjustNodes(ConfigTree actualTree);
}
