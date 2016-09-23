package org.netbeans.gradle.project;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.WaitableSignal;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.model.ModelLoader;
import org.netbeans.gradle.project.model.ModelRetrievedListener;
import org.netbeans.gradle.project.tasks.GradleDaemonManager;
import org.netbeans.gradle.project.util.NbSupplier;

public final class ProjectModelUpdater<M> {
    private final NbSupplier<? extends ModelLoader<? extends M>> modelLoaderProvider;
    private final ModelRetrievedListener<M> modelUpdaterWrapper;

    private final AtomicBoolean hasModelBeenLoaded;
    private final WaitableSignal loadedAtLeastOnceSignal;
    private final AtomicReference<Object> lastInProgressRef;

    public ProjectModelUpdater(
            NbSupplier<? extends ModelLoader<? extends M>> modelLoaderProvider,
            final ModelRetrievedListener<? super M> modelUpdater) {
        ExceptionHelper.checkNotNullArgument(modelLoaderProvider, "modelLoaderProvider");
        ExceptionHelper.checkNotNullArgument(modelUpdater, "modelUpdater");

        this.modelLoaderProvider = modelLoaderProvider;
        this.modelUpdaterWrapper = new ModelRetrievedListener<M>() {
            @Override
            public void updateModel(M model, Throwable error) {
                try {
                    modelUpdater.updateModel(model, error);
                } finally {
                    loadedAtLeastOnceSignal.signal();
                }
            }
        };

        this.hasModelBeenLoaded = new AtomicBoolean(false);
        this.loadedAtLeastOnceSignal = new WaitableSignal();
        this.lastInProgressRef = new AtomicReference<>(null);
    }

    private ModelLoader<? extends M> getModelLoader() {
        return modelLoaderProvider.get();
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

        getModelLoader().fetchModel(mayUseCache, modelUpdaterWrapper, new Runnable() {
            @Override
            public void run() {
                lastInProgressRef.compareAndSet(progressRef, null);
            }
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
