package org.netbeans.gradle.project.api.entry;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.netbeans.gradle.project.WaitableSignal;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class SingleModelExtension implements GradleProjectExtension {
    private final Class<?> requestedModel;
    private final WaitableSignal loadedSignal;
    private volatile Object lastModel;

    public SingleModelExtension(Class<?> requestedModel) {
        if (requestedModel == null) throw new NullPointerException("requestedModel");
        this.requestedModel = requestedModel;
        this.loadedSignal = new WaitableSignal();
        this.lastModel = null;
    }

    @Override
    public String getExtensionName() {
        return getClass().getName();
    }

    @Override
    public Iterable<List<Class<?>>> getGradleModels() {
        return Collections.singleton(Collections.<Class<?>>singletonList(requestedModel));
    }

    @Override
    public Lookup getExtensionLookup() {
        return Lookups.singleton(this);
    }

    @Override
    public Set<String> modelsLoaded(Lookup modelLookup) {
        lastModel = modelLookup.lookup(requestedModel);
        loadedSignal.signal();
        return Collections.emptySet();
    }

    @Override
    public Map<File, Lookup> deduceModelsForProjects(Lookup modelLookup) {
        return Collections.emptyMap();
    }

    public Object getModel(long timeout, TimeUnit unit) throws TimeoutException {
        if (!loadedSignal.tryWaitForSignal(timeout, unit)) {
            throw new TimeoutException();
        }
        return lastModel;
    }
}
