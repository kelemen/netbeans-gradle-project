package org.netbeans.gradle.project.api.entry;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.concurrent.WaitableSignal;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class SingleModelExtension<T> implements GradleProjectExtension2<T> {
    private final WaitableSignal loadedSignal;
    private volatile T lastModel;

    public SingleModelExtension() {
        this.loadedSignal = new WaitableSignal();
        this.lastModel = null;
    }

    @Override
    public Lookup getPermanentProjectLookup() {
        return Lookups.singleton(this);
    }

    @Override
    public Lookup getProjectLookup() {
        return Lookup.EMPTY;
    }

    @Override
    public void activateExtension(T parsedModel) {
        lastModel = parsedModel;
        loadedSignal.signal();
    }

    @Override
    public void deactivateExtension() {
    }

    @Override
    public Lookup getExtensionLookup() {
        return Lookup.EMPTY;
    }

    public T getModel(long timeout, TimeUnit unit) throws TimeoutException {
        if (!loadedSignal.tryWaitSignal(Cancellation.UNCANCELABLE_TOKEN, timeout, unit)) {
            throw new TimeoutException();
        }
        return lastModel;
    }
}
