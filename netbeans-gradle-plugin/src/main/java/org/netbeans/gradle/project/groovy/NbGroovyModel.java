package org.netbeans.gradle.project.groovy;

import java.io.ObjectStreamException;
import java.io.Serializable;

public final class NbGroovyModel implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final NbGroovyModel DEFAULT = new NbGroovyModel();

    private NbGroovyModel() {
    }

    private Object readResolve() throws ObjectStreamException {
        return DEFAULT;
    }
}
