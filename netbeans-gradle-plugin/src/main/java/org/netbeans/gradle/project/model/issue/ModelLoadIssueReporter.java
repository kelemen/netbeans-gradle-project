package org.netbeans.gradle.project.model.issue;

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Icon;
import javax.swing.JComponent;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.SwingConstants;
import javax.swing.SwingUtilities;
import org.jtrim2.utils.ExceptionHelper;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.model.util.Exceptions;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbIcons;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.openide.awt.NotificationDisplayer;

public final class ModelLoadIssueReporter {
    private static final Logger LOGGER = Logger.getLogger(ModelLoadIssueReporter.class.getName());
    private static final Icon ERROR_ICON = NbIcons.getPriorityHighIcon();
    private static final Color TRANSPARENT_COLOR = new Color(0, 0, 0, 0);

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
        JPanel detailsComponent = new JPanel(new FlowLayout());
        detailsComponent.setOpaque(false);
        detailsComponent.setBackground(TRANSPARENT_COLOR);
        detailsComponent.add(IssueDetailsPanel.createShowStackTraceButton(caption, detailsContent));
        return detailsComponent;
    }

    private static JLabel errorBalloonLabel(String message, final String detailsCaption, final String details) {
        String htmlMessage = "<html>" + message + "</html>";
        JLabel label = new JLabel(
                htmlMessage,
                NbIcons.getUIErrorIcon(),
                SwingConstants.LEADING);

        label.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent e) {
                IssueDetailsPanel.showModalDialog(detailsCaption, details);
            }
        });

        return label;
    }

    private static void reportAllIssuesNow(String message, Collection<? extends ModelLoadIssue> issues) {
        assert SwingUtilities.isEventDispatchThread();

        if (issues.isEmpty()) {
            return;
        }

        String details = createDetails(issues);

        JLabel messageLabel = errorBalloonLabel(message, message, details);
        JComponent detailsComponent = createDetailsComponent(message, details);

        NotificationDisplayer.getDefault().notify(
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

        ExceptionHelper.checkNotNullElements(issues, "issues");

        final List<ModelLoadIssue> issuesCopy = CollectionUtils.copyNullSafeList(issues);
        SwingUtilities.invokeLater(() -> {
            reportAllIssuesNow(message, issuesCopy);
        });
    }

    private static String setToString(Set<String> strings) {
        return strings.size() == 1
                ? strings.iterator().next()
                : strings.toString();
    }

    private static Set<String> getProjectNames(List<ModelLoadIssue> issues) {
        Set<String> names = new LinkedHashSet<>();
        for (ModelLoadIssue issue: issues) {
            names.add(issue.getRequestedProject().getDisplayName());
        }
        return names;
    }

    private static Set<String> getExtensionNames(List<ModelLoadIssue> issues) {
        Set<String> names = new LinkedHashSet<>();
        for (ModelLoadIssue issue: issues) {
            NbGradleExtensionRef extensionRef = issue.getExtensionRef();

            String name = extensionRef != null
                    ? extensionRef.getDisplayName()
                    : NbStrings.getCoreGradlePlugin();

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

        final String message = NbStrings.getInternalExtensionErrorInProject(extensionName, projectName);
        SwingUtilities.invokeLater(() -> {
            reportAllIssuesNow(message, issuesCopy);
        });
    }

    private static void reportErrorNow(String message, String detailsCaption, Throwable error) {
        assert SwingUtilities.isEventDispatchThread();

        String details = getStackTrace(error);

        JLabel messageLabel = errorBalloonLabel(message, detailsCaption, details);
        JComponent detailsComponent = createDetailsComponent(detailsCaption, details);

        NotificationDisplayer.getDefault().notify(
                message,
                ERROR_ICON,
                messageLabel,
                detailsComponent,
                NotificationDisplayer.Priority.HIGH);
    }

    private static void reportBuildScriptErrorNow(NbGradleProject project, Throwable error) {
        String projectName = project.getDisplayName();
        String message = NbStrings.getBuildScriptErrorInProject(projectName);
        reportErrorNow(message, projectName, error);
    }

    @SuppressWarnings("ThrowableResultIgnored")
    public static void reportBuildScriptError(NbGradleProject project, Throwable error) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(error, "error");

        SwingUtilities.invokeLater(() -> {
            reportBuildScriptErrorNow(project, error);
        });
    }

    private static Set<String> getFailedDependencyProjectNames(List<DependencyResolutionIssue> issues) {
        Set<String> names = new LinkedHashSet<>();
        for (DependencyResolutionIssue issue: issues) {
            names.add(issue.getProjectName());
        }
        return names;
    }

    private static String getImportantCause(DependencyResolutionIssue issue) {
        Throwable cause = issue.getStackTrace();
        Throwable rootCause = cause;
        while (cause != null) {
            rootCause = cause;
            if (Exceptions.isExceptionOfSimpleType(cause, "ModuleVersionNotFoundException")) {
                break;
            }

            cause = cause.getCause();
        }

        String message = Exceptions.getActualMessage(rootCause);
        if (message == null) {
            return "?";
        }
        return message
                .replace("\r\n", " ")
                .replace('\r', ' ')
                .replace('\n', ' ');
    }

    private static void reportDependencyResolutionFailures(List<DependencyResolutionIssue> issues) {
        assert SwingUtilities.isEventDispatchThread();

        String projectName = setToString(getFailedDependencyProjectNames(issues));
        String message = NbStrings.getDependencyResolutionFailure(projectName);

        StringBuilder detailsContent = new StringBuilder(1024);
        for (DependencyResolutionIssue issue: issues) {
            String issueMessage = issue.getMessage();
            LOGGER.log(Level.INFO, issueMessage, issue.getStackTrace());

            detailsContent.append("- ");
            detailsContent.append(issueMessage);
            detailsContent.append(" (");
            detailsContent.append(getImportantCause(issue));
            detailsContent.append(")\n");
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

        String details = detailsContent.toString();
        String detailsCaption = message;

        JLabel messageLabel = errorBalloonLabel(message, detailsCaption, details);
        JComponent detailsComponent = createDetailsComponent(detailsCaption, details);

        NotificationDisplayer.getDefault().notify(
                message,
                ERROR_ICON,
                messageLabel,
                detailsComponent,
                NotificationDisplayer.Priority.HIGH);
    }

    public static void reportDependencyResolutionFailures(Collection<? extends DependencyResolutionIssue> issues) {
        final List<DependencyResolutionIssue> issuesCopy = CollectionUtils.copyNullSafeList(issues);
        SwingUtilities.invokeLater(() -> {
            reportDependencyResolutionFailures(issuesCopy);
        });
    }

    public static boolean reportIfBuildScriptError(NbGradleProject project, Throwable error) {
        Throwable currentError = error;
        while (currentError != null) {
            if (Exceptions.isExceptionOfType(currentError, "org.gradle.api.GradleScriptException")
                    || Exceptions.isExceptionOfType(currentError, "org.gradle.api.ProjectConfigurationException")) {
                ModelLoadIssueReporter.reportBuildScriptError(project, error);
                return true;
            }
            currentError = currentError.getCause();
        }
        return false;
    }

    private ModelLoadIssueReporter() {
        throw new AssertionError();
    }
}
