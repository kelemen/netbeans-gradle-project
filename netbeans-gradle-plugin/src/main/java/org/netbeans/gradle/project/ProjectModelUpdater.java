package org.netbeans.gradle.project;

import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.concurrent.WaitableSignal;
import org.netbeans.gradle.project.model.ModelLoader;
import org.netbeans.gradle.project.model.ModelRetrievedListener;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;

public final class ProjectModelUpdater<M> {
    private final ModelLoader<? extends M>  modelLoader;
    private final ModelRetrievedListener<M> modelUpdaterWrapper;

    private final AtomicBoolean hasModelBeenLoaded;
    private final WaitableSignal loadedAtLeastOnceSignal;
    private final AtomicReference<Object> lastInProgressRef;

    public ProjectModelUpdater(
            ModelLoader<? extends M> modelLoader,
            final ModelRetrievedListener<? super M> modelUpdater) {
        Objects.requireNonNull(modelUpdater, "modelUpdater");

        this.modelLoader = Objects.requireNonNull(modelLoader, "modelLoader");
        this.loadedAtLeastOnceSignal = new WaitableSignal();

        this.modelUpdaterWrapper = (M model, Throwable error) -> {
            try {
                modelUpdater.updateModel(model, error);
            } finally {
                loadedAtLeastOnceSignal.signal();
            }
        };

        this.hasModelBeenLoaded = new AtomicBoolean(false);
        this.lastInProgressRef = new AtomicReference<>(null);
    }

    public boolean wasModelEverSet() {
        return loadedAtLeastOnceSignal.isSignaled();
    }

    public void ensureLoadRequested() {
        loadProject(true, true);
    }

    public void reloadProject() {
        loadProject(false, false);
    }

    public void reloadProjectMayUseCache() {
        loadProject(false, true);
    }

    private void loadProject(final boolean onlyIfNotLoaded, final boolean mayUseCache) {
        if (!hasModelBeenLoaded.compareAndSet(false, true)) {
            if (onlyIfNotLoaded) {
                return;
            }
        }

        final Object progressRef = new Object();
        if (mayUseCache) {
            Object currentProgressRef;
            do {
                currentProgressRef = lastInProgressRef.get();
                if (currentProgressRef != null) {
                    // Since we are content with a cached value, we consider that the
                    // model loading currently in progress will be (or was) good enough
                    // as a cached value.
                    return;
                }
            } while (!lastInProgressRef.compareAndSet(null, progressRef));
        }
        else {
            lastInProgressRef.set(progressRef);
        }

        modelLoader.fetchModel(mayUseCache, modelUpdaterWrapper, () -> {
            lastInProgressRef.compareAndSet(progressRef, null);
        });
    }

    private static void checkCanWaitForProjectLoad() {
        if (GradleDaemonManager.isRunningExclusiveTask()) {
            throw new IllegalStateException("Cannot wait for loading a project"
                    + " while blocking daemon tasks from being executed."
                    + " Possible dead-lock.");
        }
    }

    public boolean tryWaitForLoadedProject(long timeout, TimeUnit unit) {
        return tryWaitForLoadedProject(Cancellation.UNCANCELABLE_TOKEN, timeout, unit);
    }

    public boolean tryWaitForLoadedProject(CancellationToken cancelToken, long timeout, TimeUnit unit) {
        checkCanWaitForProjectLoad();

        ensureLoadRequested();
        return loadedAtLeastOnceSignal.tryWaitSignal(cancelToken, timeout, unit);
    }

    public void waitForLoadedProject(CancellationToken cancelToken) {
        checkCanWaitForProjectLoad();

        ensureLoadRequested();
        loadedAtLeastOnceSignal.waitSignal(cancelToken);
    }
}
