package org.netbeans.gradle.project.java.test;

import java.util.Objects;

public final class SpecificTestClass {
    private final String testClassName;

    public SpecificTestClass(String testClassName) {
        this.testClassName = Objects.requireNonNull(testClassName, "testClassName");
    }

    public String getTestClassName() {
        return testClassName;
    }
}
