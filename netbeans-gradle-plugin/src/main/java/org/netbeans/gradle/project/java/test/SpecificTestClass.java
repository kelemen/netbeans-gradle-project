package org.netbeans.gradle.project.java.test;

public final class SpecificTestClass {
    private final String testClassName;

    public SpecificTestClass(String testClassName) {
        if (testClassName == null) throw new NullPointerException("testClassName");
        this.testClassName = testClassName;
    }

    public String getTestClassName() {
        return testClassName;
    }
}
