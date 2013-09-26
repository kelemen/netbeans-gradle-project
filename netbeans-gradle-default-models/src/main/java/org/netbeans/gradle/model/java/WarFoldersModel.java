package org.netbeans.gradle.model.java;

import java.io.File;
import java.io.Serializable;

public final class WarFoldersModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final File webAppDir;

    public WarFoldersModel(File webAppDir) {
        if (webAppDir == null) throw new NullPointerException("webAppDir");
        this.webAppDir = webAppDir;
    }

    public File getWebAppDir() {
        return webAppDir;
    }
}
