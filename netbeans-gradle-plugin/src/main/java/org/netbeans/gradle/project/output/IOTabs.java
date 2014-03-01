package org.netbeans.gradle.project.output;

import java.awt.Dialog;
import java.awt.event.ActionEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import javax.swing.Action;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.api.task.GradleCommandTemplate;
import org.netbeans.gradle.project.properties.PredefinedTask;
import org.netbeans.gradle.project.tasks.AsyncGradleTask;
import org.netbeans.gradle.project.tasks.GradleCommandSpec;
import org.netbeans.gradle.project.tasks.GradleTaskDef;
import org.netbeans.gradle.project.tasks.TaskOutputKey;
import org.netbeans.gradle.project.view.CustomActionPanel;
import org.openide.DialogDescriptor;
import org.openide.DialogDisplayer;
import org.openide.util.ImageUtilities;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public final class IOTabs {
    private static final IOTabMaintainer<TaskOutputKey, TaskIOTab> TASK_TABS
            = new IOTabMaintainer<>(createTaskIOTabFactory());

    private static IOTabFactory<TaskIOTab> createTaskIOTabFactory() {
        return new IOTabFactory<TaskIOTab>() {
            @Override
            public TaskIOTab create(String caption) {
                TaskTabAction[] actions = createActions();
                InputOutput io = IOProvider.getDefault().getIO(caption, actions);
                return new TaskIOTab(io, actions);
            }
        };
    }

    private static TaskTabAction[] createActions() {
        return new TaskTabAction[] {
            new ReRunTask(),
            new ReRunWithDifferentArgsTask()
        };
    }

    public static IOTabMaintainer<TaskOutputKey, TaskIOTab> taskTabs() {
        return TASK_TABS;
    }

    @SuppressWarnings("serial")
    private static class ReRunTask extends TaskTabAction {
        @StaticResource
        private static final String ICON = "org/netbeans/gradle/project/resources/rerun-icon.png";

        public ReRunTask() {
            putValue(Action.SMALL_ICON, ImageUtilities.loadImage(ICON));

            putValue(Action.NAME, NbStrings.getReRunName());
            putValue(Action.SHORT_DESCRIPTION, NbStrings.getReRunDescription());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            AsyncGradleTask task = getLastTask();
            if (task != null) {
                task.run();
            }
        }
    }

    @SuppressWarnings("serial")
    private static class ReRunWithDifferentArgsTask extends TaskTabAction {
        @StaticResource
        private static final String ICON = "org/netbeans/gradle/project/resources/rerun-diff-args-icon.png";

        public ReRunWithDifferentArgsTask() {
            putValue(Action.SMALL_ICON, ImageUtilities.loadImage(ICON));

            putValue(Action.NAME, NbStrings.getReRunDiffName());
            putValue(Action.SHORT_DESCRIPTION, NbStrings.getReRunDiffDescription());
        }

        private static PredefinedTask toPredefined(GradleTaskDef taskDef) {
            List<String> sourceNames = taskDef.getTaskNames();
            List<PredefinedTask.Name> names = new ArrayList<>(sourceNames.size());
            for (String name: sourceNames) {
                names.add(new PredefinedTask.Name(name, false));
            }

            return new PredefinedTask(
                    taskDef.getCommandName(),
                    names,
                    taskDef.getArguments(),
                    taskDef.getJvmArguments(),
                    false);
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            GradleTaskDef lastSource = getLastSourceTask();
            AsyncGradleTask task = getLastTask();
            if (task != null && lastSource != null) {
                CustomActionPanel panel = new CustomActionPanel(false);
                panel.updatePanel(toPredefined(lastSource));

                DialogDescriptor dlgDescriptor = new DialogDescriptor(
                        panel,
                        NbStrings.getCustomTaskDlgTitle(),
                        true,
                        new Object[]{DialogDescriptor.OK_OPTION, DialogDescriptor.CANCEL_OPTION},
                        DialogDescriptor.OK_OPTION,
                        DialogDescriptor.BOTTOM_ALIGN,
                        null,
                        null);
                Dialog dlg = DialogDisplayer.getDefault().createDialog(dlgDescriptor);
                dlg.pack();
                dlg.setVisible(true);
                if (dlgDescriptor.getValue() != DialogDescriptor.OK_OPTION) {
                    return;
                }

                GradleCommandTemplate template
                        = panel.tryGetGradleCommand(lastSource.getCommandName());
                AsyncGradleTask newTask = new AsyncGradleTask(
                        task.getProject(),
                        new CommandAdjusterFactory(task.getTaskDefFactroy(), template),
                        task.getListener());
                newTask.run();
            }
        }
    }

    private static final class CommandAdjusterFactory implements Callable<GradleCommandSpec> {
        private final Callable<GradleCommandSpec> source;
        private final List<String> taskNames;
        private final List<String> arguments;
        private final List<String> jvmArguments;

        public CommandAdjusterFactory(
                Callable<GradleCommandSpec> source,
                GradleCommandTemplate template) {

            this.source = source;
            this.taskNames = template.getTasks();
            this.arguments = template.getArguments();
            this.jvmArguments = template.getJvmArguments();
        }

        @Override
        public GradleCommandSpec call() throws Exception {
            GradleCommandSpec original = source.call();
            if (original == null) {
                return null;
            }

            GradleTaskDef.Builder result = new GradleTaskDef.Builder(original.getSource());
            result.setTaskNames(taskNames);
            result.setArguments(arguments);
            result.setJvmArguments(jvmArguments);

            return new GradleCommandSpec(result.create(), null);
        }
    }

    private IOTabs() {
        throw new AssertionError();
    }
}
