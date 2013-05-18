package org.netbeans.gradle.project.java.model;

/**
 * Defines an immutable model of a J2SE Gradle project.
 */
public final class NbJavaModel {
    private final NbJavaModule mainModule;

    public NbJavaModel(
            NbJavaModule mainModule) {
        if (mainModule == null) throw new NullPointerException("mainModule");

        this.mainModule = mainModule;
    }

    public NbJavaModule getMainModule() {
        return mainModule;
    }
}
