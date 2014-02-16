package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;

public final class JavaTestTask implements Serializable {
    private static final long serialVersionUID = 1L;

    private final String name;
    private final File xmlOutputDir;

    public JavaTestTask(String name, File xmlOutputDir) {
        if (name == null) throw new NullPointerException("name");
        if (xmlOutputDir == null) throw new NullPointerException("xmlOutputDir");

        this.name = name;
        this.xmlOutputDir = xmlOutputDir;
    }

    public String getName() {
        return name;
    }

    public File getXmlOutputDir() {
        return xmlOutputDir;
    }
}
