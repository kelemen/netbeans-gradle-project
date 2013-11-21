package org.netbeans.gradle.project.view;

import javax.swing.Icon;
import javax.swing.JLabel;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.openide.awt.NotificationDisplayer;

public final class GlobalErrorReporter {
    private static final Icon ERROR_ICON = NbIcons.getUIErrorIcon();

    public static void showIssue(final String message, Throwable error) {
        if (message == null) throw new NullPointerException("message");

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                SwingUtilities.invokeLater(new Runnable() {
                    @Override
                    public void run() {
                        NotificationDisplayer displayer = NotificationDisplayer.getDefault();
                        JLabel messageLabel = new JLabel(message);
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
        });
    }

    private GlobalErrorReporter() {
        throw new AssertionError();
    }
}
