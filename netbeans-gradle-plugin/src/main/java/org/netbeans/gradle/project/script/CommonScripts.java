package org.netbeans.gradle.project.script;

import java.nio.file.Path;
import java.util.Objects;

public final class CommonScripts {
    public static final String DEFAULT_SCRIPT_EXTENSION = GroovyScripts.EXTENSION;

    public static final String SETTINGS_BASE_NAME = "settings";
    public static final String BUILD_BASE_NAME = "build";

    public static final String BUILD_SRC_NAME = "buildSrc";

    public static final String GRADLE_PROPERTIES_NAME = "gradle.properties";

    private final ScriptFileProvider scriptProvider;

    public CommonScripts(ScriptFileProvider scriptProvider) {
        this.scriptProvider = Objects.requireNonNull(scriptProvider, "scriptProvider");
    }

    public Path getScriptFilePath(Path baseDir, String baseName) {
        Path result = scriptProvider.findScriptFile(baseDir, baseName);
        if (result != null) {
            return result;
        }
        return baseDir.resolve(baseName + DEFAULT_SCRIPT_EXTENSION);
    }
}
