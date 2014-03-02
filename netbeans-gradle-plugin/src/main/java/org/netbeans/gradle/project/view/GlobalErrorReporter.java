package org.netbeans.gradle.project.view;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.openide.awt.NotificationDisplayer;

public final class GlobalErrorReporter {
    private static final Icon ERROR_ICON = NbIcons.getPriorityHighIcon();

    public static void showIssue(final String message) {
        ExceptionHelper.checkNotNullArgument(message, "message");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                NotificationDisplayer displayer = NotificationDisplayer.getDefault();
                JLabel messageLabel = new JLabel(
                        message,
                        NbIcons.getUIErrorIcon(),
                        SwingConstants.LEADING);
                JLabel lineLabel = new JLabel(message);

                displayer.notify(
                        NbStrings.getGlobalErrorReporterTitle(),
                        ERROR_ICON,
                        messageLabel,
                        lineLabel,
                        NotificationDisplayer.Priority.HIGH);
            }
        });
    }

    private GlobalErrorReporter() {
        throw new AssertionError();
    }
}
