
package org.netbeans.gradle.project.java.model;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import org.netbeans.gradle.model.java.JacocoModel;

public final class NbCodeCoverage implements Serializable {
    private static final long serialVersionUID = 1L;

    public static final NbCodeCoverage NO_CODE_COVERAGE = new NbCodeCoverage(null);

    private final JacocoModel jacocoModel;

    public NbCodeCoverage(JacocoModel jacocoModel) {
        this.jacocoModel = jacocoModel;
    }

    public JacocoModel tryGetJacocoModel() {
        return jacocoModel;
    }

    public boolean hasCodeCoverage() {
        return jacocoModel != null;
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final JacocoModel jacocoModel;

        public SerializedFormat(NbCodeCoverage source) {
            this.jacocoModel = source.jacocoModel;
        }

        private Object readResolve() throws ObjectStreamException {
            return new NbCodeCoverage(jacocoModel);
        }
    }
}
