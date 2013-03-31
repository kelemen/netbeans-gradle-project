package org.netbeans.gradle.project.others;

import java.lang.reflect.Method;
import org.netbeans.api.project.Project;

/**
 *
 * @author Kelemen Attila
 */
public final class ChangeLFPlugin {
    private static final PluginLookupMethod LINE_FEED_METHOD;

    static {
        PluginClass pluginClass = new PluginClass(
                "com.junichi11.netbeans.changelf",
                "com.junichi11.netbeans.changelf.api.ChangeLF");
        LINE_FEED_METHOD = new PluginLookupMethod(pluginClass, "getCurrentLineFeedCode", Project.class);
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
}
