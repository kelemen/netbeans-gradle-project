package org.netbeans.gradle.model.java;

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
}
