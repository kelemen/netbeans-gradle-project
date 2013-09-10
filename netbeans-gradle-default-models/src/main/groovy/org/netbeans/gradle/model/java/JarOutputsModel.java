package org.netbeans.gradle.model.java;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

public final class JarOutputsModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final Collection<JarOutput> jars;

    public JarOutputsModel(Collection<JarOutput> jars) {
        this.jars = Collections.unmodifiableList(new ArrayList<JarOutput>(jars));
    }

    public Collection<JarOutput> getJars() {
        return jars;
    }
}
