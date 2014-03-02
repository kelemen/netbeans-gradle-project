package org.netbeans.gradle.project.java.test;

import org.jtrim.utils.ExceptionHelper;

public final class SpecificTestcase {
    private final String testClassName;
    private final String testMethodName;

    public SpecificTestcase(String testClassName, String testMethodName) {
        ExceptionHelper.checkNotNullArgument(testClassName, "testClassName");
        ExceptionHelper.checkNotNullArgument(testMethodName, "testMethodName");

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
