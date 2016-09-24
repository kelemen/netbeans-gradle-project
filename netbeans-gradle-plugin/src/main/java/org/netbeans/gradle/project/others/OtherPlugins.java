package org.netbeans.gradle.project.others;

import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.openide.util.Lookup;

public final class OtherPlugins {
    public static final PluginClassFactory CHANGE_LF
            = new PluginClassFactory("com.junichi11.netbeans.changelf");

    public static final String JAVA_EE_EXTENSION_NAME = "org.netbeans.gradle.javaee.web.WebModuleExtensionDef";
    private static volatile Boolean hasJavaEE = null;

    private static boolean checkJavaEEExtension() {
        for (GradleProjectExtensionDef<?> extDef: Lookup.getDefault().lookupAll(GradleProjectExtensionDef.class)) {
            if (JAVA_EE_EXTENSION_NAME.equals(extDef.getName())) {
                return true;
            }
        }
        return false;
    }

    public static boolean hasJavaEEExtension() {
        Boolean result = hasJavaEE;
        if (result == null) {
            result = checkJavaEEExtension();
            hasJavaEE = result;
        }
        return result;
    }

    public static boolean hasJavaEEExtension(NbGradleProject project) {
        return project.getExtensions().hasExtension(JAVA_EE_EXTENSION_NAME);
    }

    private OtherPlugins() {
        throw new AssertionError();
    }
}
