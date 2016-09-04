package org.netbeans.gradle.project.properties;

import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;

public final class ScriptPlatform {
    private final JavaPlatform javaPlatform;
    private final PlatformSelectionMode selectionMode;

    public ScriptPlatform(JavaPlatform javaPlatform, PlatformSelectionMode selectionMode) {
        ExceptionHelper.checkNotNullArgument(javaPlatform, "javaPlatform");
        ExceptionHelper.checkNotNullArgument(selectionMode, "selectionMode");

        this.javaPlatform = javaPlatform;
        this.selectionMode = selectionMode;
    }

    public static ScriptPlatform getDefault() {
        return ConstHolder.DEFAULT;
    }

    public JavaPlatform getJavaPlatform() {
        return javaPlatform;
    }

    public PlatformSelectionMode getSelectionMode() {
        return selectionMode;
    }

    @Override
    public String toString() {
        return "ScriptPlatform{" + "spec=" + javaPlatform.getSpecification() + ", selectionMode=" + selectionMode + '}';
    }

    private static class ConstHolder {
        // We want to avoid the risk of trying to get the default platform too quickly
        // to prevent the chance of NB not being ready for providing it.
        public static final ScriptPlatform DEFAULT
                = new ScriptPlatform(JavaPlatform.getDefault(), PlatformSelectionMode.BY_LOCATION);
    }
}
