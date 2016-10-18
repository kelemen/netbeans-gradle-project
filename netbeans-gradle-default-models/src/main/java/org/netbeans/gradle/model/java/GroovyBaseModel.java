package org.netbeans.gradle.model.java;

import java.io.ObjectStreamException;
import java.io.Serializable;

public final class GroovyBaseModel implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final GroovyBaseModel DEFAULT = new GroovyBaseModel();

    private GroovyBaseModel() {
    }

    private Object readResolve() throws ObjectStreamException {
        return DEFAULT;
    }
}
