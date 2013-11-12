package org.netbeans.gradle.project.others;

public final class OtherPlugins {
    public static final PluginClassFactory CHANGE_LF
            = new PluginClassFactory("com.junichi11.netbeans.changelf");

    public static final PluginClassFactory WEB_BEANS
            = new PluginClassFactory("org.netbeans.modules.web.beans/2");

    private OtherPlugins() {
        throw new AssertionError();
    }
}
