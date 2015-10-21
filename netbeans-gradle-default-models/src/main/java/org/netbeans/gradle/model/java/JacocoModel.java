package org.netbeans.gradle.model.java;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;

public final class JacocoModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final JacocoReportFiles report;

    public JacocoModel(JacocoReportFiles report) {
        if (report == null) throw new NullPointerException("report");

        this.report = report;
    }

    public JacocoReportFiles getReport() {
        return report;
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final JacocoReportFiles report;

        public SerializedFormat(JacocoModel source) {
            this.report = source.report;
        }

        private Object readResolve() throws ObjectStreamException {
            return new JacocoModel(report);
        }
    }
}
