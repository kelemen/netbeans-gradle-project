package org.netbeans.gradle.model.util;

import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.BuilderIssue;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;

public final class BuilderUtils {
    public static String getNameForEnumBuilder(Enum<?> instance) {
        return instance.getClass().getSimpleName() + '.' + instance.name();
    }

    public static String getNameForGenericBuilder(Object instance, String args) {
        return instance.getClass().getSimpleName() + '(' + args + ')';
    }

    private static String getSafeToString(Object obj) {
        String className = obj.getClass().getSimpleName();
        String toStringValue = obj.toString();

        if (toStringValue == null) {
            return className;
        }
        else if (toStringValue.contains(className)) {
            return toStringValue;
        }
        else {
            return className + ": " + toStringValue;
        }
    }

    private static String getNameOfBuilder(ProjectInfoBuilder<?> builder) {
        if (builder == null) {
            return "null";
        }

        String name = builder.getName();
        // Although getName should never return null,
        // do something sensible anyway just to be safe.
        return name != null
                ? name
                : getSafeToString(builder);
    }


    public static BuilderIssue createIssue(
            ProjectInfoBuilder<?> builder,
            Throwable issue) {
        if (issue == null) {
            return null;
        }

        return new BuilderIssue(getNameOfBuilder(builder), issue);
    }

    private static String getNameOfBuilder(BuildInfoBuilder<?> builder) {
        if (builder == null) {
            return "null";
        }

        String name = builder.getName();
        // Although getName should never return null,
        // do something sensible anyway just to be safe.
        return name != null
                ? name
                : getSafeToString(builder);
    }


    public static BuilderIssue createIssue(
            BuildInfoBuilder<?> builder,
            Throwable issue) {
        if (issue == null) {
            return null;
        }

        return new BuilderIssue(getNameOfBuilder(builder), issue);
    }

    private BuilderUtils() {
        throw new AssertionError();
    }
}
