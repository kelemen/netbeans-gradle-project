package org.netbeans.gradle.project.util;

import java.util.Objects;
import org.jtrim2.concurrent.Tasks;
import org.jtrim2.event.ListenerRef;

public final class EventUtils {
    public static ListenerRef asSafeListenerRef(Runnable task) {
        Objects.requireNonNull(task, "task");
        return Tasks.runOnceTask(task)::run;
    }
}
