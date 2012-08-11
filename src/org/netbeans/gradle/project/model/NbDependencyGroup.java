package org.netbeans.gradle.project.model;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class NbDependencyGroup {
    public static final NbDependencyGroup EMPTY = new NbDependencyGroup(
            Collections.<NbModuleDependency>emptyList(),
            Collections.<NbUriDependency>emptyList());

    private final List<NbModuleDependency> moduleDependencies;
    private final List<NbUriDependency> uriDependencies;

    public NbDependencyGroup(
            List<NbModuleDependency> moduleDependencies,
            List<NbUriDependency> uriDependencies) {
        if (moduleDependencies == null) throw new NullPointerException("moduleDependencies");
        if (uriDependencies == null) throw new NullPointerException("uriDependencies");

        this.moduleDependencies = Collections.unmodifiableList(
                new ArrayList<NbModuleDependency>(moduleDependencies));
        this.uriDependencies = Collections.unmodifiableList(
                new ArrayList<NbUriDependency>(uriDependencies));

        for (NbDependency dependency: this.moduleDependencies) {
            if (dependency == null) throw new NullPointerException("dependency");
        }
        for (NbDependency dependency: this.uriDependencies) {
            if (dependency == null) throw new NullPointerException("dependency");
        }
    }

    public List<NbModuleDependency> getModuleDependencies() {
        return moduleDependencies;
    }

    public List<NbUriDependency> getUriDependencies() {
        return uriDependencies;
    }

    public List<NbDependency> getAllDependencies() {
        List<NbDependency> result = new ArrayList<NbDependency>(
                moduleDependencies.size() + uriDependencies.size());
        result.addAll(moduleDependencies);
        result.addAll(uriDependencies);
        return result;
    }

    public boolean isEmpty() {
        return moduleDependencies.isEmpty() && uriDependencies.isEmpty();
    }
}
