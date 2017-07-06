package org.netbeans.gradle.project.java.test;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Objects;
import javax.swing.AbstractAction;
import javax.swing.Action;
import javax.swing.SwingUtilities;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.output.StackTraceConsumer;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.netbeans.modules.gsf.testrunner.ui.api.CallstackFrameNode;

public final class JavaCallstackFrameNode extends CallstackFrameNode {
    private final JavaExtension javaExt;

    public JavaCallstackFrameNode(String frameInfo, String displayName, JavaExtension javaExt) {
        super(frameInfo, displayName);

        this.javaExt = Objects.requireNonNull(javaExt, "javaExt");
    }

    @Override
    public Action getPreferredAction() {
        return getJumpToSourceAction();
    }

    private Action getJumpToSourceAction() {
        return new JumpToSourceAction();
    }

    public static ActionListener tryGetOpenLocationAction(Project project, String location) {
        String stackTraceLine = getConsumableStackTrace(location);

        StackTraceConsumer stackTraceConsumer = new StackTraceConsumer(project);
        return stackTraceConsumer.tryGetOpenEditorAction(stackTraceLine);
    }

    private static String getConsumableStackTrace(String line) {
        if (line.contains(" at ")) {
            return line;
        }
        if (line.startsWith("at ")) {
            return " " + line;
        }
        return " at " + line;
    }

    @SuppressWarnings("serial")
    private class JumpToSourceAction extends AbstractAction {
        public JumpToSourceAction() {
            super(NbStrings.getJumpToSource());
        }

        private void doActionNow(final ActionEvent e) {
            if (frameInfo == null) {
                return;
            }

            final ActionListener action = tryGetOpenLocationAction(javaExt.getProject(), frameInfo);
            if (action != null) {
                SwingUtilities.invokeLater(() -> {
                    action.actionPerformed(e);
                });
            }
        }

        @Override
        public void actionPerformed(final ActionEvent e) {
            NbTaskExecutors.DEFAULT_EXECUTOR.execute(() -> doActionNow(e));
        }
    }
}
