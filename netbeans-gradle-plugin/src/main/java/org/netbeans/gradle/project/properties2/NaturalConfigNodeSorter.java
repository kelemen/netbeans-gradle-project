package org.netbeans.gradle.project.properties2;

public final class NaturalConfigNodeSorter implements ConfigNodeSorter {
    public static final NaturalConfigNodeSorter INSTANCE = new NaturalConfigNodeSorter();

    @Override
    public ConfigNodeSorter getChildSorter(String keyName) {
        return NaturalConfigNodeSorter.INSTANCE;
    }

    @Override
    public int compare(String o1, String o2) {
        return o1.compareTo(o2);
    }
}
