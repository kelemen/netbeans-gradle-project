package org.netbeans.gradle.project.others.test;

import org.netbeans.api.project.Project;
import org.netbeans.modules.gsf.testrunner.api.RerunHandler;
import org.netbeans.modules.gsf.testrunner.ui.api.TestRunnerNodeFactory;

public interface NbGradleTestManager {
    public NbGradleTestSession startSession(
            String name,
            Project project,
            TestRunnerNodeFactory nodeFactory,
            RerunHandler rerunHandler);
}
