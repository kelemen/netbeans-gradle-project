package org.netbeans.gradle.project.model.issue;

import java.awt.GridLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.openide.awt.NotificationDisplayer;

public final class ModelLoadIssueReporter {
    private static final Logger LOGGER = Logger.getLogger(ModelLoadIssueReporter.class.getName());
    private static final Icon ERROR_ICON = NbIcons.getPriorityHighIcon();

    private static void printStackTrace(Throwable error, Writer output) {
        PrintWriter writer = new PrintWriter(output);
        try {
            error.printStackTrace(writer);
        } finally {
            writer.flush();
        }
    }

    private static String getStackTrace(Throwable error) {
        StringWriter output = new StringWriter();
        printStackTrace(error, output);
        return output.toString();
    }

    private static String indentExceptFirstLine(String str) {
        return str.replace("\n", "\n    ");
    }

    private static JComponent createDetailsComponent(Collection<? extends ModelLoadIssue> issues) {
        // TODO: Create something better: The stack trace must be hidden by default.
        // TODO: I18N
        StringBuilder details = new StringBuilder();
        int index = 1;
        for (ModelLoadIssue issue: issues) {
            if (details.length() > 0) {
                details.append("\n\n");
            }

            details.append("Issue ");
            details.append(index);
            details.append('\n');

            details.append("--------\n\n");

            details.append("  Description: ");
            details.append(indentExceptFirstLine(issue.getIssueDescription()));

            details.append("\n\n  Stack trace:\n    ");
            details.append(indentExceptFirstLine(getStackTrace(issue.getStackTrace())));

            index++;
        }

        String detailsContent = details.toString();
        final JTextArea textArea = new JTextArea(detailsContent);
        textArea.setEditable(false);
        textArea.setRows(5);
        textArea.setColumns(10);

        JPanel result = new JPanel(new GridLayout(1, 1));
        result.add(new JScrollPane(textArea));
        return result;
    }

    private static void reportAllIssuesNow(String message, Collection<? extends ModelLoadIssue> issues) {
        assert SwingUtilities.isEventDispatchThread();

        if (issues.isEmpty()) {
            return;
        }

        String htmlMessage = "<html>" + message + "</html>";

        NotificationDisplayer displayer = NotificationDisplayer.getDefault();
        JLabel messageLabel = new JLabel(
                htmlMessage,
                NbIcons.getUIErrorIcon(),
                SwingConstants.LEADING);

        JComponent detailsComponent = createDetailsComponent(issues);

        displayer.notify(
                NbStrings.getGlobalErrorReporterTitle(),
                ERROR_ICON,
                messageLabel,
                detailsComponent,
                NotificationDisplayer.Priority.HIGH);
    }

    private static List<ModelLoadIssue> getSortedCopy(Collection<? extends ModelLoadIssue> issues) {
        List<ModelLoadIssue> sorted = new ArrayList<ModelLoadIssue>(issues);
        Collections.sort(sorted, new Comparator<ModelLoadIssue>() {
            @Override
            public int compare(ModelLoadIssue o1, ModelLoadIssue o2) {
                return o2.getSeverity().getValue() - o1.getSeverity().getValue();
            }
        });
        CollectionUtils.checkNoNullElements(sorted, "issues[sorted]");
        return sorted;
    }

    private static void logIssues(Collection<? extends ModelLoadIssue> issues) {
        for (ModelLoadIssue issue: issues) {
            if (issue != null) {
                LOGGER.log(Level.INFO,
                        "Model load issue: " + issue.getIssueDescription(),
                        issue.getStackTrace());
            }
        }
    }

    public static void reportAllIssues(
            final String message,
            Collection<? extends ModelLoadIssue> issues) {

        logIssues(issues);
        if (message == null) throw new NullPointerException("message");

        final List<ModelLoadIssue> sortedIssues = getSortedCopy(issues);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                reportAllIssuesNow(message, sortedIssues);
            }
        });
    }

    private static String getDefaultMessage(Collection<? extends ModelLoadIssue> issues) {
        if (issues.isEmpty()) {
            return "";
        }

        ModelLoadIssue worstProblem = issues.iterator().next();

        // TODO: I18N
        String subProblem;
        switch (worstProblem.getSeverity()) {
            case INTERNAL_ERROR:
                subProblem = "This is most likely a bug in the Gradle Support plugin.";
                break;
            case EXTENSION_ERROR:
                subProblem = "This is most likely a bug in an extension.";
                break;
            case BUILD_SCRIPT_ERROR:
                subProblem = "There was an error in the build scripts of the evaluated project.";
                break;
            default:
                subProblem = worstProblem.getSeverity().name();
                break;
        }

        return "Failed to properly evaluate build scripts. " + subProblem;
    }

    public static void reportAllIssues(Collection<? extends ModelLoadIssue> issues) {
        if (issues.isEmpty()) {
            return;
        }

        logIssues(issues);

        final List<ModelLoadIssue> sortedIssues = getSortedCopy(issues);
        final String message = getDefaultMessage(sortedIssues);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                reportAllIssuesNow(message, sortedIssues);
            }
        });
    }

    private ModelLoadIssueReporter() {
        throw new AssertionError();
    }
}
