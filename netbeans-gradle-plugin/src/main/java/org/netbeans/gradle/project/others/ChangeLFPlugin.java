package org.netbeans.gradle.project.others;

import java.lang.reflect.Method;
import org.netbeans.api.project.Project;
import org.netbeans.spi.project.ui.support.ProjectCustomizer;

public final class ChangeLFPlugin {
    private static final PluginLookupMethod LINE_FEED_METHOD;
    private static final PluginLookupMethod PROJECT_SETTINGS_METHOD;

    static {
        PluginClass pluginClass = new PluginClass(
                OtherPlugins.CHANGE_LF,
                "com.junichi11.netbeans.changelf.api.ChangeLF");

        LINE_FEED_METHOD = new PluginLookupMethod(pluginClass, "getCurrentLineFeedCode", Project.class);
        PROJECT_SETTINGS_METHOD = new PluginLookupMethod(pluginClass, "getCompositCategoryProvider");
    }

    public static String getPreferredLineSeparator(Project project) {
        Object lineFeedEnum = LINE_FEED_METHOD.tryCall(project);
        if (lineFeedEnum == null) {
            return null;
        }

        Method lineFeedStrMethod = ReflectionHelper.tryGetMethod(lineFeedEnum.getClass(), "getLineSeparator");
        if (lineFeedStrMethod == null) {
            return null;
        }

        Object lineFeedStr = ReflectionHelper.tryInvoke(lineFeedStrMethod, lineFeedEnum);
        return lineFeedStr != null ? lineFeedStr.toString() : null;
    }

    public static ProjectCustomizer.CompositeCategoryProvider getProjectSettings() {
        Object category = PROJECT_SETTINGS_METHOD.tryCall();
        if (category instanceof ProjectCustomizer.CompositeCategoryProvider) {
            return (ProjectCustomizer.CompositeCategoryProvider)category;
        }
        else {
            return null;
        }
    }
}
