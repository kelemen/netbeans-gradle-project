package org.netbeans.gradle.project.script;

public final class KotlinScripts {
    public static final String EXTENSION_WITHOUT_DOT = "gradle.kts";
    public static final String EXTENSION = "." + EXTENSION_WITHOUT_DOT;

    private KotlinScripts() {
        throw new AssertionError();
    }
}
