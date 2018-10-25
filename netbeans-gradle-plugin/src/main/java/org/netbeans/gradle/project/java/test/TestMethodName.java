package org.netbeans.gradle.project.java.test;

import java.util.Objects;
import org.netbeans.modules.gsf.testrunner.api.Testcase;

public final class TestMethodName {
    private static final char[] TEST_NAME_TERMINATE_CHARS = "([".toCharArray();

    private final String testName;

    public TestMethodName(String testName) {
        this.testName = Objects.requireNonNull(testName, "testName");
    }

    public static SpecificTestcase tryConvertToSpecificTestcase(Testcase testcase) {
        String className = testcase.getClassName();
        String name = testcase.getName();

        if (className != null && name != null) {
            return new SpecificTestcase(className, new TestMethodName(name));
        }
        else {
            return null;
        }
    }

    public String getTestName() {
        return testName;
    }

    public String getGradleArgName() {
        return getRawMethodName();
    }

    public String getRawMethodName() {
        return extractTestMethodName(testName);
    }

    private static String extractTestMethodName(String testName) {
        int minIndex = Integer.MAX_VALUE;
        for (char endCh: TEST_NAME_TERMINATE_CHARS) {
            int index = testName.indexOf(endCh);
            if (index >= 0 && index < minIndex) {
                minIndex = index;
            }
        }
        return minIndex >= testName.length() ? testName : testName.substring(0, minIndex);
    }
}
