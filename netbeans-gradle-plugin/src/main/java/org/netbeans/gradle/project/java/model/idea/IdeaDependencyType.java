package org.netbeans.gradle.project.java.model.idea;

public enum IdeaDependencyType {
    COMPILE,
    PROVIDED_COMPILE,
    RUNTIME,
    TEST_COMPILE,
    TEST_RUNTIME,
    OTHER;

    public static IdeaDependencyType fromIdeaScope(String scope) {
        if ("COMPILE".equalsIgnoreCase(scope)) {
            return IdeaDependencyType.COMPILE;
        }
        else if ("PROVIDED".equalsIgnoreCase(scope)) {
            return IdeaDependencyType.PROVIDED_COMPILE;
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
