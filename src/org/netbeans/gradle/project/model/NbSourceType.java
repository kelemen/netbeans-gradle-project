package org.netbeans.gradle.project.model;

public enum NbSourceType {
    SOURCE(false, false),
    TEST_SOURCE(true, false),
    RESOURCE(false, true),
    TEST_RESOURCE(true, true),
    OTHER(false, true);

    private final boolean test;
    private final boolean resource;

    private NbSourceType(boolean test, boolean resource) {
        this.test = test;
        this.resource = resource;
    }

    public boolean isTest() {
        return test;
    }

    public boolean isResource() {
        return resource;
    }
}
