package org.netbeans.gradle.project.view;

import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbStrings;

public final class GlobalErrorReporter {
    private static boolean showingIssue = false;

    public static void showIssue(final String message, Throwable error) {
        if (message == null) throw new NullPointerException("message");

        // TODO: This should be able to remember multiple issues like when
        //   logging WARNING messages.
        //   It would be better to use the nice bubbles and problem log of NB 7.4.

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (showingIssue) {
                    return;
                }

                showingIssue = true;
                try {
                    JOptionPane.showMessageDialog(null,
                            message,
                            NbStrings.getGlobalErrorReporterTitle(),
                            JOptionPane.ERROR_MESSAGE);
                } finally {
                    showingIssue = false;
                }
            }
        });
    }

    private GlobalErrorReporter() {
        throw new AssertionError();
    }
}
