package org.netbeans.gradle.project.properties2;

import java.util.Comparator;

public interface ConfigNodeSorter extends Comparator<String> {
    public ConfigNodeSorter getChildSorter(String keyName);
}
