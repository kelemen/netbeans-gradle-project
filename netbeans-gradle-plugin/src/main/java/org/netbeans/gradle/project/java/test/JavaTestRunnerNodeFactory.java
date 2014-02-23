package org.netbeans.gradle.project.java.test;

import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.modules.gsf.testrunner.api.CallstackFrameNode;
import org.netbeans.modules.gsf.testrunner.api.TestMethodNode;
import org.netbeans.modules.gsf.testrunner.api.TestRunnerNodeFactory;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.api.TestsuiteNode;
import org.openide.nodes.Node;

public final class JavaTestRunnerNodeFactory extends TestRunnerNodeFactory {
    private final JavaExtension javaExt;

    public JavaTestRunnerNodeFactory(JavaExtension javaExt) {
        if (javaExt == null) throw new NullPointerException("javaExt");
        this.javaExt = javaExt;
    }

    @Override
    public Node createTestMethodNode(Testcase testcase, Project project) {
        return new TestMethodNode(testcase, project);
    }

    @Override
    public Node createCallstackFrameNode(String frameInfo, String displayName) {
        return new CallstackFrameNode(frameInfo, displayName);
    }

    @Override
    public TestsuiteNode createTestSuiteNode(String suiteName, boolean filtered) {
        return new TestsuiteNode(suiteName, filtered);
    }
}
