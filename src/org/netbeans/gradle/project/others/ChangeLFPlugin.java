package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import org.netbeans.api.project.Project;
import org.openide.modules.ModuleInfo;
import org.openide.util.Lookup;

/**
 *
 * @author Kelemen Attila
 */
public final class ChangeLFPlugin {
    private static volatile Class<?> CHANGE_LF_CLASS = null;

    private static Class<?> tryGetLookupClass() {
        Class<?> result = CHANGE_LF_CLASS;
        if (result == null) {
            try {
                for (ModuleInfo info: Lookup.getDefault().lookupAll(ModuleInfo.class)) {
                    String codeName = info.getCodeName();
                    if (codeName != null && codeName.startsWith("com.junichi11.netbeans.changelf")) {
                        result = Class.forName("com.junichi11.netbeans.changelf.api.ChangeLF", true, info.getClassLoader());
                        CHANGE_LF_CLASS = result;
                    }
                }
            } catch (ClassNotFoundException ex) {
            }
        }
        return result;
    }

    private static Method tryGetMethod(Class<?> cl, String methodName, Class<?>... args) {
        try {
            return cl.getMethod(methodName, args);
        } catch (NoSuchMethodException ex) {
            return null;
        }
    }

    private static Object tryInvoke(Method method, Object obj, Object... args) {
        try {
            return method.invoke(obj, args);
        } catch (IllegalAccessException ex) {
        } catch (IllegalArgumentException ex) {
        } catch (InvocationTargetException ex) {
        }
        return null;
    }

    public static String getPreferredLineSeparator() {
        Class<?> lookupClass = tryGetLookupClass();
        if (lookupClass == null) {
            return null;
        }

        Object lookup = Lookup.getDefault().lookup(lookupClass);
        if (lookup == null) {
            return null;
        }

        Method lineFeedMethod = tryGetMethod(lookupClass, "getCurrentLineFeedCode", Project.class);
        if (lineFeedMethod == null) {
            return null;
        }

        Object lineFeedEnum = tryInvoke(lineFeedMethod, lookup, (Object)null);
        if (lineFeedEnum == null) {
            return null;
        }

        Method lineFeedStrMethod = tryGetMethod(lineFeedEnum.getClass(), "getLineSeparator");
        if (lineFeedStrMethod == null) {
            return null;
        }

        Object lineFeedStr = tryInvoke(lineFeedStrMethod, lineFeedEnum);
        return lineFeedStr != null ? lineFeedStr.toString() : null;
    }
}
