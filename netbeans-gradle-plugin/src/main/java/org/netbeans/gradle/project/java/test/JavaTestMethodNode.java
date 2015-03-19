package org.netbeans.gradle.project.java.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.progress.ProgressHandle;
import org.netbeans.api.progress.ProgressHandleFactory;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.view.GradleActionProvider;
import org.netbeans.modules.gsf.testrunner.api.TestMethodNode;
import org.netbeans.modules.gsf.testrunner.api.Testcase;
import org.netbeans.modules.gsf.testrunner.api.Trouble;
import org.netbeans.spi.project.SingleMethod;
import org.openide.util.Cancellable;
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

        ExceptionHelper.checkNotNullArgument(javaExt, "javaExt");
        ExceptionHelper.checkNotNullArgument(testTaskName, "testTaskName");

        this.javaExt = javaExt;
        this.testTaskName = testTaskName;
        this.specificTestcase = toSpecificTestcase(testcase);
    }

    private static SpecificTestcase toSpecificTestcase(Testcase testcase) {
        String className = testcase.getClassName();
        String name = testcase.getName();

        if (className != null && name != null) {
            return new SpecificTestcase(className, name);
        }
        else {
            return null;
        }
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
        if (specificTestcase == null) {
            return null;
        }

        String className = specificTestcase.getTestClassName();
        String methodName = specificTestcase.getTestMethodName();
        return className + '.' + methodName;
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
                    SwingUtilities.invokeLater(new Runnable() {
                        @Override
                        public void run() {
                            if (!cancelToken.isCanceled()) {
                                openInEditorAction.actionPerformed(e);
                            }
                        }
                    });
                }
            }
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            final CancellationSource cancel = Cancellation.createCancellationSource();
            final ProgressHandle progress = ProgressHandleFactory.createHandle(NbStrings.getJumpToSource(), new Cancellable() {
                @Override
                public boolean cancel() {
                    cancel.getController().cancel();
                    return true;
                }
            });

            NbTaskExecutors.DEFAULT_EXECUTOR.execute(cancel.getToken(), new CancelableTask() {
                @Override
                public void execute(CancellationToken cancelToken) {
                    progress.start();
                    try {
                        doActionNow(cancelToken, e);
                    } finally {
                        progress.finish();
                    }
                }
            }, null);
        }
    }
}
