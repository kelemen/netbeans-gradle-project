package org.netbeans.gradle.project.java.test;

import java.util.Objects;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.ui.api.TestRunnerNodeFactory;
import org.netbeans.modules.gsf.testrunner.ui.api.TestsuiteNode;
import org.openide.nodes.Node;

public final class JavaTestRunnerNodeFactory extends TestRunnerNodeFactory {
    private final JavaExtension javaExt;
    private final TestTaskName testTaskName;

    public JavaTestRunnerNodeFactory(JavaExtension javaExt, TestTaskName testTaskName) {
        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.testTaskName = Objects.requireNonNull(testTaskName, "testTaskName");
    }

    @Override
    public Node createTestMethodNode(Testcase testcase, Project project) {
        return new JavaTestMethodNode(testcase, javaExt, testTaskName);
    }

    @Override
    public Node createCallstackFrameNode(String frameInfo, String displayName) {
        return new JavaCallstackFrameNode(frameInfo, displayName, javaExt);
    }

    @Override
    public TestsuiteNode createTestSuiteNode(String suiteName, boolean filtered) {
        return new JavaTestsuiteNode(suiteName, filtered, javaExt, testTaskName);
    }
}
