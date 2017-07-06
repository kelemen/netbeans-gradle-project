package org.netbeans.gradle.project.view;

import java.util.Objects;
import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.openide.awt.NotificationDisplayer;

public final class GlobalErrorReporter {
    private static final Icon ERROR_ICON = NbIcons.getPriorityHighIcon();

    public static void showIssue(final String message, final Icon icon) {
        Objects.requireNonNull(message, "message");

        SwingUtilities.invokeLater(() -> {
            NotificationDisplayer displayer = NotificationDisplayer.getDefault();
            JLabel messageLabel = new JLabel(
                    message,
                    icon,
                    SwingConstants.LEADING);
            JLabel lineLabel = new JLabel(message);

            displayer.notify(
                    NbStrings.getGlobalErrorReporterTitle(),
                    ERROR_ICON,
                    messageLabel,
                    lineLabel,
                    NotificationDisplayer.Priority.HIGH);
        });
    }

    public static void showIssue(String message) {
        showIssue(message, NbIcons.getUIErrorIcon());
    }

    public static void showWarning(String message) {
        showIssue(message, NbIcons.getUIWarningIcon());
    }

    private GlobalErrorReporter() {
        throw new AssertionError();
    }
}
