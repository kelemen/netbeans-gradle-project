package org.netbeans.gradle.project.java.test;

import org.jtrim.utils.ExceptionHelper;
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
        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");
        ExceptionHelper.checkNotNullArgument(testTaskName, "testTaskName");

        this.javaExt = javaExt;
        this.testTaskName = testTaskName;
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
