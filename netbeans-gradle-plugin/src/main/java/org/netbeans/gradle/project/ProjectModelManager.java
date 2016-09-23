
package org.netbeans.gradle.project;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.SwingUtilities;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.property.PropertySource;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.event.ChangeListenerManager;
import org.netbeans.gradle.project.event.GenericChangeListenerManager;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.netbeans.gradle.project.model.ModelRefreshListener;
import org.netbeans.gradle.project.model.ModelRetrievedListener;
import org.netbeans.gradle.project.model.NbGradleModel;
import org.netbeans.gradle.project.model.ProjectModelChangeListener;
import org.netbeans.gradle.project.properties.NbProperties;
import org.netbeans.gradle.project.query.GradleCacheBinaryForSourceQuery;
import org.netbeans.gradle.project.query.GradleCacheByBinaryLookup;
import org.netbeans.gradle.project.util.LazyValue;
import org.netbeans.gradle.project.util.NbSupplier;

final class ProjectModelManager implements ModelRetrievedListener<NbGradleModel> {
    private static final Logger LOGGER = Logger.getLogger(ProjectModelManager.class.getName());

    private final NbGradleProject project;
    private final ChangeListenerManager modelChangeListeners;
    private final AtomicReference<NbGradleModel> currentModelRef;
    private final PropertySource<NbGradleModel> currentModel;
    private final LazyValue<ProjectInfoRef> loadErrorRef;
    private final UpdateTaskExecutor modelUpdater;
    private final Runnable modelUpdateDispatcher;

    public ProjectModelManager(final NbGradleProject project, final NbGradleModel initialModel) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(initialModel, "initialModel");

        this.project = project;
        this.modelChangeListeners = GenericChangeListenerManager.getSwingNotifier();
        this.currentModelRef = new AtomicReference<>(initialModel);
        this.currentModel = NbProperties.atomicValueView(currentModelRef, modelChangeListeners);
        this.modelUpdater = new SwingUpdateTaskExecutor(true);
        this.modelUpdateDispatcher = new Runnable() {
            @Override
            public void run() {
                onModelChange();
            }
        };
        this.loadErrorRef = new LazyValue<>(new NbSupplier<ProjectInfoRef>() {
            @Override
            public ProjectInfoRef get() {
                return project.getProjectInfoManager().createInfoRef();
            }
        });
    }

    private void onModelChange() {
        assert SwingUtilities.isEventDispatchThread();
        try {
            modelChangeListeners.fireEventually();
            for (ProjectModelChangeListener listener: project.getLookup().lookupAll(ProjectModelChangeListener.class)) {
                listener.onModelChanged();
            }
        } finally {
            GradleCacheByBinaryLookup.notifyCacheChange();
            GradleCacheBinaryForSourceQuery.notifyCacheChange();
        }
    }

    public PropertySource<NbGradleModel> currentModel() {
        return currentModel;
    }

    private void fireModelChangeEvent() {
        modelUpdater.execute(modelUpdateDispatcher);
    }

    private boolean safelyLoadExtensions(NbGradleExtensionRef extension, Object model) {
        try {
            return extension.setModelForExtension(model);
        } catch (Throwable ex) {
            LOGGER.log(Level.SEVERE, "Extension has thrown an unexpected exception: " + extension.getName(), ex);
            return false;
        }
    }

    private boolean notifyEmptyModelChange() {
        boolean changedAny = false;
        for (NbGradleExtensionRef extensionRef: project.getExtensionRefs()) {
            boolean changed = safelyLoadExtensions(extensionRef, null);
            changedAny = changedAny || changed;
        }
        fireModelChangeEvent();
        return changedAny;
    }

    private boolean notifyModelChange(NbGradleModel model) {
        // TODO: Consider conflicts
        //   GradleProjectExtensionDef.getSuppressedExtensions()
        boolean changedAny = false;
        for (NbGradleExtensionRef extensionRef: project.getExtensionRefs()) {
            boolean changed = safelyLoadExtensions(extensionRef, model.getModelOfExtension(extensionRef));
            changedAny = changedAny || changed;
        }
        fireModelChangeEvent();
        return changedAny;
    }

    private void startRefresh(Collection<ModelRefreshListener> listeners) {
        for (ModelRefreshListener listener: listeners) {
            try {
                listener.startRefresh();
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Failed to call " + listener.getClass().getName() + ".startRefresh()", ex);
            }
        }
    }

    private void endRefresh(Collection<ModelRefreshListener> listeners, boolean extensionsChanged) {
        for (ModelRefreshListener listener: listeners) {
            try {
                listener.endRefresh(extensionsChanged);
            } catch (Throwable ex) {
                LOGGER.log(Level.SEVERE, "Failed to call " + listener.getClass().getName() + ".endRefresh(" + extensionsChanged + ")", ex);
            }
        }
    }

    private void updateExtensionActivation(NbGradleModel model) {
        Collection<ModelRefreshListener> refreshListeners = new ArrayList<>(project.getLookup().lookupAll(ModelRefreshListener.class));
        boolean extensionsChanged = false;
        startRefresh(refreshListeners);
        try {
            if (model == null) {
                extensionsChanged = notifyEmptyModelChange();
            }
            else {
                extensionsChanged = notifyModelChange(model);
            }
        } finally {
            endRefresh(refreshListeners, extensionsChanged);
        }
    }

    private ProjectInfoRef getLoadErrorRef() {
        return loadErrorRef.get();
    }

    public void updateModel(NbGradleModel model) {
        updateModel(model, null);
    }

    @Override
    public void updateModel(NbGradleModel model, Throwable error) {
        boolean hasChanged = false;
        if (model != null) {
            NbGradleModel prevModel = currentModelRef.getAndSet(model);
            hasChanged = prevModel != model;
        }
        if (error != null) {
            ProjectInfo.Entry entry = new ProjectInfo.Entry(ProjectInfo.Kind.ERROR, NbStrings.getErrorLoadingProject(error));
            getLoadErrorRef().setInfo(new ProjectInfo(Collections.singleton(entry)));
            LOGGER.log(Level.INFO, "Error while loading the project model.", error);
            project.displayError(NbStrings.getProjectLoadFailure(project.getName()), error);
        }
        else {
            getLoadErrorRef().setInfo(null);
        }
        if (hasChanged) {
            updateExtensionActivation(model);
        }
    }

}
