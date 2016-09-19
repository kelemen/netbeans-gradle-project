package org.netbeans.gradle.model.util;

import org.netbeans.gradle.model.BuildInfoBuilder;
import org.netbeans.gradle.model.BuilderIssue;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;

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

    private static String getNameOfBuilderUnsafe(ProjectInfoBuilder2<?> builder) {
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

    private static String getNameOfBuilder(ProjectInfoBuilder2<?> builder, Throwable issue) {
        try {
            return getNameOfBuilderUnsafe(builder);
        } catch (Throwable ex) {
            Exceptions.tryAddSuppressedException(issue, ex);
            return builder != null ? builder.getClass().getName() : "null";
        }
    }

    public static BuilderIssue createIssue(
            ProjectInfoBuilder2<?> builder,
            Throwable issue) {
        if (issue == null) {
            return null;
        }

        return new BuilderIssue(getNameOfBuilder(builder, issue), issue);
    }

    private static String getNameOfBuilderUnsafe(BuildInfoBuilder<?> builder) {
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

    private static String getNameOfBuilder(BuildInfoBuilder<?> builder, Throwable issue) {
        try {
            return getNameOfBuilderUnsafe(builder);
        } catch (Throwable ex) {
            Exceptions.tryAddSuppressedException(issue, ex);
            return builder != null ? builder.getClass().getName() : "null";
        }
    }

    public static BuilderIssue createIssue(
            BuildInfoBuilder<?> builder,
            Throwable issue) {
        if (issue == null) {
            return null;
        }

        return new BuilderIssue(getNameOfBuilder(builder, issue), issue);
    }

    private BuilderUtils() {
        throw new AssertionError();
    }
}
