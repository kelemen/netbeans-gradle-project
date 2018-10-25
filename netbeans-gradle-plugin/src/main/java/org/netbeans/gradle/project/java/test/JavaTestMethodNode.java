package org.netbeans.gradle.project.java.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.AsyncTasks;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.api.Trouble;
import org.netbeans.modules.gsf.testrunner.ui.api.TestMethodNode;
import org.netbeans.spi.project.SingleMethod;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class JavaTestMethodNode extends TestMethodNode {
    private final JavaExtension javaExt;
    private final TestTaskName testTaskName;
    private final SpecificTestcase specificTestcase;

    public JavaTestMethodNode(
            Testcase testcase,
            JavaExtension javaExt,
            TestTaskName testTaskName) {
        super(testcase, javaExt.getProject());

        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
        this.testTaskName = Objects.requireNonNull(testTaskName, "testTaskName");
        this.specificTestcase = TestMethodName.tryConvertToSpecificTestcase(testcase);
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            getJumpToSourceAction(),
            runMethodAction(),
            debugMethodAction(),
        };
    }

    @Override
    public Action getPreferredAction() {
        return getJumpToSourceAction();
    }

    private Action runMethodAction() {
        return new RerunTestMethodAction(
                NbStrings.getTestSingleMethodAgain(),
                SingleMethod.COMMAND_RUN_SINGLE_METHOD);
    }

    private Action debugMethodAction() {
        return new RerunTestMethodAction(
                NbStrings.getDebugTestSingleMethodAgain(),
                SingleMethod.COMMAND_DEBUG_SINGLE_METHOD);
    }

    private Action getJumpToSourceAction() {
        return new JumpToSourceAction();
    }

    private String tryGetQualifiedName() {
        return specificTestcase != null
                ? specificTestcase.getQualifiedName()
                : null;
    }

    private String[] tryGetStackTrace() {
        Trouble trouble = testcase.getTrouble();
        if (trouble == null) {
            return null;
        }

        return trouble.getStackTrace();
    }

    private ActionListener tryGetOpenEditorActionAtFailure() {
        String[] stackTrace = tryGetStackTrace();
        String qualifiedName = tryGetQualifiedName();
        if (stackTrace == null || qualifiedName == null) {
            return null;
        }

        for (String location: stackTrace) {
            if (location != null && location.contains(qualifiedName)) {
                return JavaCallstackFrameNode.tryGetOpenLocationAction(javaExt.getProject(), location);
            }
        }

        return null;
    }

    private boolean openTestMethod(CancellationToken cancelToken) {
        if (specificTestcase != null) {
            return ShowTestUtils.openTestMethod(cancelToken, javaExt, specificTestcase);
        }
        else {
            return false;
        }
    }

    @SuppressWarnings("serial")
    private class RerunTestMethodAction extends AbstractAction {
        private final String command;

        public RerunTestMethodAction(String name, String command) {
            super(name);

            this.command = command;
            if (specificTestcase == null) {
                setEnabled(false);
            }
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            if (specificTestcase == null) {
                return;
            }

            Project project = javaExt.getProject();
            Lookup context = Lookups.fixed(testTaskName, specificTestcase);

            GradleActionProvider.invokeAction(project, command, context);
        }
    }

    @SuppressWarnings("serial")
    private class JumpToSourceAction extends AbstractAction {
        public JumpToSourceAction() {
            super(NbStrings.getJumpToSource());
        }

        private void doActionNow(final CancellationToken cancelToken, final ActionEvent e) {
            if (!openTestMethod(cancelToken)) {
                final ActionListener openInEditorAction = tryGetOpenEditorActionAtFailure();
                if (openInEditorAction != null) {
                    SwingUtilities.invokeLater(() -> {
                        if (!cancelToken.isCanceled()) {
                            openInEditorAction.actionPerformed(e);
                        }
                    });
                }
            }
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final CancellationSource cancel = Cancellation.createCancellationSource();
            final ProgressHandle progress = ProgressHandle.createHandle(NbStrings.getJumpToSource(), () -> {
                cancel.getController().cancel();
                return true;
            });

            NbTaskExecutors.DEFAULT_EXECUTOR.execute(cancel.getToken(), (CancellationToken cancelToken) -> {
                progress.start();
                try {
                    doActionNow(cancelToken, e);
                } finally {
                    progress.finish();
                }
            }).exceptionally(AsyncTasks::expectNoError);
        }
    }
}
