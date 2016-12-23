package org.netbeans.gradle.project.script;

public final class GroovyScripts {
    public static final String EXTENSION_WITHOUT_DOT = "gradle";
    public static final String EXTENSION = "." + EXTENSION_WITHOUT_DOT;

    private GroovyScripts() {
        throw new AssertionError();
    }
}
