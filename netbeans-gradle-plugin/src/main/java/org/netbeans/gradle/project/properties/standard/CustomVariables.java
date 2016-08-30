package org.netbeans.gradle.project.properties.standard;

import java.util.Collection;

public interface CustomVariables {
    public String tryGetValue(String name);
    public Collection<CustomVariable> getVariables();
    public boolean isEmpty();
}
