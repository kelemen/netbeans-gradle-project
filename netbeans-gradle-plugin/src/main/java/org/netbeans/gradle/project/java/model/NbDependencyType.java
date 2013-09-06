package org.netbeans.gradle.project.java.model;

public enum NbDependencyType {
    COMPILE,
    RUNTIME,
    TEST_COMPILE,
    TEST_RUNTIME,
    OTHER;

    public static NbDependencyType fromIdeaScope(String scope) {
        if ("COMPILE".equalsIgnoreCase(scope) || "PROVIDED".equalsIgnoreCase(scope)) {
            return NbDependencyType.COMPILE;
        }
        else if ("TEST".equalsIgnoreCase(scope)) {
            return NbDependencyType.TEST_COMPILE;
        }
        else if ("RUNTIME".equalsIgnoreCase(scope)) {
            return NbDependencyType.RUNTIME;
        }
        else {
            return NbDependencyType.OTHER;
        }
    }
}
