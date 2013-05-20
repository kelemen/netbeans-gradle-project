package org.netbeans.gradle.project.api.entry;

import com.google.common.annotations.VisibleForTesting;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import org.openide.util.Lookup;
import org.openide.util.lookup.AbstractLookup;
import org.openide.util.lookup.InstanceContent;

/**
 *
 * @author radim
 */
public class GradleTestExtension implements GradleProjectExtension {

    @VisibleForTesting final CountDownLatch loadedSignal = new CountDownLatch(1);
    
    private final InstanceContent ic;
    private final Lookup lookup;

    public GradleTestExtension() {
        ic = new InstanceContent();
        lookup = new AbstractLookup(ic);
        ic.add(this);
    }
    
    @Override
    public Iterable<List<Class<?>>> getGradleModels() {
        return Collections.<List<Class<?>>>singleton(Collections.<Class<?>>emptyList());
    }

    @Override
    public Lookup getExtensionLookup() {
        return lookup;
    }

    @Override
    public void modelsLoaded(Lookup modelLookup) {
        loadedSignal.countDown();
    }
    
}
