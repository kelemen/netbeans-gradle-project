package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;

public final class JacocoReportFiles implements Serializable {
    private static final long serialVersionUID = 1L;

    private final File html;
    private final File xml;

    public JacocoReportFiles(File html, File xml) {
        if (html == null) throw new NullPointerException("html");
        if (xml == null) throw new NullPointerException("xml");

        this.html = html;
        this.xml = xml;
    }

    public File getHtml() {
        return html;
    }

    public File getXml() {
        return xml;
    }
}
