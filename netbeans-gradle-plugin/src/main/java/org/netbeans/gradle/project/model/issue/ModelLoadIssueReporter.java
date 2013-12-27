package org.netbeans.gradle.project.model.issue;

import java.awt.FlowLayout;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleExtensionRef;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.openide.awt.NotificationDisplayer;
import org.openide.util.Parameters;

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

    private static String createDetails(Collection<? extends ModelLoadIssue> issues) {
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

            details.append("  Requested project: ");
            details.append(issue.getRequestedProject().getProjectDirectoryAsFile());
            if (issue.getActualProjectPath() != null) {
                details.append("\n  Actual project: ");
                details.append(issue.getActualProjectPath());
            }
            if (issue.getExtensionName() != null) {
                details.append("\n  Extension: ");
                details.append(issue.getExtensionName());
            }
            if (issue.getBuilderName()!= null) {
                details.append("\n  Model builder: ");
                details.append(issue.getBuilderName());
            }

            details.append("\n\n  Stack trace:\n    ");
            details.append(indentExceptFirstLine(getStackTrace(issue.getStackTrace())));

            index++;
        }

        return details.toString();
    }

    private static JComponent createDetailsComponent(String caption, String detailsContent) {
        JComponent detailsComponent = new JPanel(new FlowLayout());
        detailsComponent.add(IssueDetailsPanel.createShowStackTraceButton(caption, detailsContent));
        return detailsComponent;
    }

    private static JComponent createDetailsComponent(String caption, Collection<? extends ModelLoadIssue> issues) {
        return createDetailsComponent(caption, createDetails(issues));
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

        JComponent detailsComponent = createDetailsComponent(message, issues);

        displayer.notify(
                message,
                ERROR_ICON,
                messageLabel,
                detailsComponent,
                NotificationDisplayer.Priority.HIGH);
    }

    private static void logIssues(Collection<? extends ModelLoadIssue> issues) {
        for (ModelLoadIssue issue: issues) {
            if (issue != null) {
                LOGGER.log(Level.INFO,
                        "Model load issue: " + issue,
                        issue.getStackTrace());
            }
        }
    }

    public static void reportAllIssues(
            final String message,
            Collection<? extends ModelLoadIssue> issues) {

        logIssues(issues);
        if (message == null) throw new NullPointerException("message");

        final List<ModelLoadIssue> issuesCopy = CollectionUtils.copyNullSafeList(issues);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                reportAllIssuesNow(message, issuesCopy);
            }
        });
    }

    private static String setToString(Set<String> strings) {
        return strings.size() == 1
                ? strings.iterator().next()
                : strings.toString();
    }

    private static Set<String> getProjectNames(List<ModelLoadIssue> issues) {
        Set<String> names = new LinkedHashSet<String>();
        for (ModelLoadIssue issue: issues) {
            names.add(issue.getRequestedProject().getDisplayName());
        }
        return names;
    }

    private static Set<String> getExtensionNames(List<ModelLoadIssue> issues) {
        Set<String> names = new LinkedHashSet<String>();
        for (ModelLoadIssue issue: issues) {
            NbGradleExtensionRef extensionRef = issue.getExtensionRef();

            // TODO: I18N
            String name = extensionRef != null
                    ? extensionRef.getDisplayName()
                    : "Core Gradle plugin";

            names.add(name);
        }
        return names;
    }

    public static void reportAllIssues(Collection<? extends ModelLoadIssue> issues) {
        if (issues.isEmpty()) {
            return;
        }

        logIssues(issues);

        final List<ModelLoadIssue> issuesCopy = CollectionUtils.copyNullSafeList(issues);

        String projectName = setToString(getProjectNames(issuesCopy));
        String extensionName = setToString(getExtensionNames(issuesCopy));

        // TODO: I18N
        final String message = "Internal error in " + extensionName + " for project " + projectName;
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                reportAllIssuesNow(message, issuesCopy);
            }
        });
    }

    private static void reportBuildScriptErrorNow(NbGradleProject project, Throwable error) {
        assert SwingUtilities.isEventDispatchThread();

        // TODO: I18N
        String message = "The build script of " + project.getDisplayName() + " contains an error.";
        String htmlMessage = "<html>" + message + "</html>";

        NotificationDisplayer displayer = NotificationDisplayer.getDefault();
        JLabel messageLabel = new JLabel(
                htmlMessage,
                NbIcons.getUIErrorIcon(),
                SwingConstants.LEADING);

        JComponent detailsComponent = createDetailsComponent(project.getDisplayName(), getStackTrace(error));

        displayer.notify(
                message,
                ERROR_ICON,
                messageLabel,
                detailsComponent,
                NotificationDisplayer.Priority.HIGH);
    }

    public static void reportBuildScriptError(final NbGradleProject project, final Throwable error) {
        Parameters.notNull("project", project);
        Parameters.notNull("error", error);

        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                reportBuildScriptErrorNow(project, error);
            }
        });
    }

    private static Set<String> getFailedDependencyProjectNames(List<DependencyResolutionIssue> issues) {
        Set<String> names = new LinkedHashSet<String>();
        for (DependencyResolutionIssue issue: issues) {
            names.add(issue.getProjectName());
        }
        return names;
    }

    private static void reportDependencyResolutionFailures(List<DependencyResolutionIssue> issues) {
        assert SwingUtilities.isEventDispatchThread();

        String projectName = setToString(getFailedDependencyProjectNames(issues));
        String message = NbStrings.getDependencyResolutionFailure(projectName);
        String htmlMessage = "<html>" + message + "</html>";

        NotificationDisplayer displayer = NotificationDisplayer.getDefault();
        JLabel messageLabel = new JLabel(
                htmlMessage,
                NbIcons.getUIErrorIcon(),
                SwingConstants.LEADING);

        // TODO: Try to extract useful parts of the stack trace.
        StringBuilder detailsContent = new StringBuilder(1024);
        for (DependencyResolutionIssue issue: issues) {
            detailsContent.append("- ");
            detailsContent.append(issue.getMessage());
            detailsContent.append('\n');
        }

        detailsContent.append("\nDetails: \n");

        int issueIndex = 1;
        for (DependencyResolutionIssue issue: issues) {
            detailsContent.append("\n");
            detailsContent.append("Exception ");
            detailsContent.append(issueIndex);
            detailsContent.append("\n---------------\n\n");
            detailsContent.append(getStackTrace(issue.getStackTrace()));

            issueIndex++;
        }
        JComponent detailsComponent = new JPanel(new FlowLayout());
        detailsComponent.add(IssueDetailsPanel
                .createShowStackTraceButton(message, detailsContent.toString()));

        displayer.notify(
                message,
                ERROR_ICON,
                messageLabel,
                detailsComponent,
                NotificationDisplayer.Priority.HIGH);
    }

    public static void reportDependencyResolutionFailures(Collection<? extends DependencyResolutionIssue> issues) {
        final List<DependencyResolutionIssue> issuesCopy = CollectionUtils.copyNullSafeList(issues);
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                reportDependencyResolutionFailures(issuesCopy);
            }
        });
    }

    private ModelLoadIssueReporter() {
        throw new AssertionError();
    }
}
