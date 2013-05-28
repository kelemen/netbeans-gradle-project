package org.netbeans.gradle.project.api.entry;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicReference;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

/**
 *
 * @author radim
 */
public class GradleTestExtension implements GradleProjectExtension {
    @VisibleForTesting
    final CountDownLatch loadedSignal;
    private final AtomicReference<Lookup> lookupRef;

    public GradleTestExtension() {
        this.loadedSignal = new CountDownLatch(1);
        this.lookupRef = new AtomicReference<Lookup>(null);
    }

    @Override
    public Iterable<List<Class<?>>> getGradleModels() {
        return Collections.<List<Class<?>>>singleton(Collections.<Class<?>>emptyList());
    }

    @Override
    public Lookup getExtensionLookup() {
        Lookup lookup = lookupRef.get();
        if (lookup == null) {
            lookupRef.compareAndSet(null, Lookups.fixed(this));
            lookup = lookupRef.get();
        }
        return lookup;
    }

    @Override
    public Set<String> modelsLoaded(Lookup modelLookup) {
        loadedSignal.countDown();
        return null;
    }

    @Override
    public String getExtensionName() {
        return getClass().getName();
    }
}
