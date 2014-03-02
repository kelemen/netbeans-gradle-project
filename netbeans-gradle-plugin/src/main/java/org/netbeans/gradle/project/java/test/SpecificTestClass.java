package org.netbeans.gradle.project.java.test;

import org.jtrim.utils.ExceptionHelper;

public final class SpecificTestClass {
    private final String testClassName;

    public SpecificTestClass(String testClassName) {
        ExceptionHelper.checkNotNullArgument(testClassName, "testClassName");
        this.testClassName = testClassName;
    }

    public String getTestClassName() {
        return testClassName;
    }
}
