package org.netbeans.gradle.project.output;

import java.awt.event.ActionEvent;
import javax.swing.Action;
import org.netbeans.api.annotations.common.StaticResource;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.tasks.AsyncGradleTask;
import org.netbeans.gradle.project.tasks.TaskOutputKey;
import org.openide.util.ImageUtilities;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public final class IOTabs {
    private static final IOTabMaintainer<TaskOutputKey, TaskIOTab> TASK_TABS
            = new IOTabMaintainer<TaskOutputKey, TaskIOTab>(createTaskIOTabFactory());

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
            new ReRunTask()
        };
    }

    public static IOTabMaintainer<TaskOutputKey, TaskIOTab> taskTabs() {
        return TASK_TABS;
    }

    @SuppressWarnings("serial")
    private static class ReRunTask extends TaskTabAction {
        @StaticResource
        private static final String RERUN_ICON = "org/netbeans/gradle/project/resources/rerun-icon.png";

        public ReRunTask() {
            putValue(Action.SMALL_ICON, ImageUtilities.loadImage(RERUN_ICON));

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

    private IOTabs() {
        throw new AssertionError();
    }
}
