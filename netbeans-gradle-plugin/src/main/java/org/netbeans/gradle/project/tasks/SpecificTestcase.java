package org.netbeans.gradle.project.tasks;

public final class SpecificTestcase {
    private final String testClassName;
    private final String testMethodName;

    public SpecificTestcase(String testClassName, String testMethodName) {
        if (testClassName == null) throw new NullPointerException("testClassName");
        if (testMethodName == null) throw new NullPointerException("testMethodName");

        this.testClassName = testClassName;
        this.testMethodName = testMethodName;
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
