package org.netbeans.gradle.project.java.model.idea;

public enum IdeaDependencyType {
    COMPILE,
    RUNTIME,
    TEST_COMPILE,
    TEST_RUNTIME,
    OTHER;

    public static IdeaDependencyType fromIdeaScope(String scope) {
        if ("COMPILE".equalsIgnoreCase(scope) || "PROVIDED".equalsIgnoreCase(scope)) {
            return IdeaDependencyType.COMPILE;
        }
        else if ("TEST".equalsIgnoreCase(scope)) {
            return IdeaDependencyType.TEST_COMPILE;
        }
        else if ("RUNTIME".equalsIgnoreCase(scope)) {
            return IdeaDependencyType.RUNTIME;
        }
        else {
            return IdeaDependencyType.OTHER;
        }
    }
}
