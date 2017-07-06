package org.netbeans.gradle.project.java.test;

import java.util.Objects;

public final class SpecificTestcase {
    private final String testClassName;
    private final String testMethodName;

    public SpecificTestcase(String testClassName, String testMethodName) {
        this.testClassName = Objects.requireNonNull(testClassName, "testClassName");
        this.testMethodName = Objects.requireNonNull(testMethodName, "testMethodName");
    }

    public String getTestClassName() {
        return testClassName;
    }

    public String getTestMethodName() {
        return testMethodName;
    }

    public String getTestIncludePattern() {
        return testClassName + "." + testMethodName;
    }
}
