package org.netbeans.gradle.project.java.test;

import java.util.Objects;

public final class SpecificTestcase {
    private final String testClassName;
    private final TestMethodName testMethodName;

    public SpecificTestcase(String testClassName, TestMethodName testMethodName) {
        this.testClassName = Objects.requireNonNull(testClassName, "testClassName");
        this.testMethodName = Objects.requireNonNull(testMethodName, "testMethodName");
    }

    public String getTestClassName() {
        return testClassName;
    }

    public TestMethodName getTestMethodName() {
        return testMethodName;
    }

    public String getTestIncludePattern() {
        return getQualifiedName();
    }

    public String getQualifiedName() {
        return testClassName + "." + testMethodName.getGradleArgName();
    }
}
