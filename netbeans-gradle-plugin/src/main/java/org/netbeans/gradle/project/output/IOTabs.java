package org.netbeans.gradle.project.output;

import org.netbeans.gradle.project.tasks.TaskOutputKey;
import org.openide.windows.IOProvider;
import org.openide.windows.InputOutput;

public final class IOTabs {
    private static final IOTabMaintainer<TaskOutputKey, TaskIOTab> TASK_TABS
            = new IOTabMaintainer<TaskOutputKey, TaskIOTab>(createTaskIOTabFactory());

    private static IOTabFactory<TaskIOTab> createTaskIOTabFactory() {
        return new IOTabFactory<TaskIOTab>() {
            @Override
            public TaskIOTab create(String caption) {
                // TODO: Create action buttons
                InputOutput io = IOProvider.getDefault().getIO(caption, true);
                return new TaskIOTab(io);
            }
        };
    }

    public static IOTabMaintainer<TaskOutputKey, TaskIOTab> taskTabs() {
        return TASK_TABS;
    }

    private IOTabs() {
        throw new AssertionError();
    }
}
