package org.netbeans.gradle.project.util;

import javax.swing.event.ChangeListener;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.openide.util.ChangeSupport;

public final class LazyChangeSupport {
    private final ChangeSupport changeSupport;
    private final UpdateTaskExecutor eventExecutor;
    private final Runnable eventDispatcher;

    private LazyChangeSupport(Object source, TaskExecutor eventExecutor) {
        this.changeSupport = new ChangeSupport(source);
        this.eventExecutor = new GenericUpdateTaskExecutor(eventExecutor);
        this.eventDispatcher = new EventDispatcher(changeSupport);
    }

    public static LazyChangeSupport createSwing(Source source) {
        return create(source, SwingExecutors.getStrictExecutor(true));
    }

    public static LazyChangeSupport create(Source source, TaskExecutor eventExecutor) {
        LazyChangeSupport result = new LazyChangeSupport(source, eventExecutor);
        source.init(result);
        return result;
    }

    public void fireChange() {
        eventExecutor.execute(eventDispatcher);
    }

    public void addChangeListener(ChangeListener listener) {
        changeSupport.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changeSupport.removeChangeListener(listener);
    }

    public interface Source {
        public void init(LazyChangeSupport changes);
    }

    private static class EventDispatcher implements Runnable {
        private final ChangeSupport changes;

        public EventDispatcher(ChangeSupport changes) {
            this.changes = changes;
        }

        @Override
        public void run() {
            changes.fireChange();
        }
    }
}
