package org.netbeans.gradle.model.java;

import java.io.File;

public final class WarFoldersModel {
    private final File webAppDir;

    public WarFoldersModel(File webAppDir) {
        if (webAppDir == null) throw new NullPointerException("webAppDir");
        this.webAppDir = webAppDir;
    }

    public File getWebAppDir() {
        return webAppDir;
    }
}
