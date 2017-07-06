package org.netbeans.gradle.project.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.function.Consumer;
import org.jtrim2.executor.ContextAwareWrapper;
import org.jtrim2.executor.ManualTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.TaskExecutors;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class LazyProjectModelPersisterFactoryTest {
    private static void executeAll(ManualTaskExecutor executor) {
        while (executor.executeCurrentlySubmitted() > 0) {
            // One more time to execute tasks submitted by the executed tasks.
        }
    }

    private static PersistentModelStore<Object> createLazyStore(
            PersistentModelStore<Object> wrapped,
            TaskExecutor persisterExecutor) {
        LazyPersistentModelStoreFactory<Object> factory = new LazyPersistentModelStoreFactory<>(wrapped, persisterExecutor);
        return factory.createStore(wrapped);
    }

    @Test
    public void testPersistOne() throws Exception {
        MemPersistentModelStore<Object> modelStore = new MemPersistentModelStore<>();
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        PersistentModelStore<Object> persister = createLazyStore(modelStore, executor);

        Object model = "MyModel";
        Path dest = Paths.get("MyTestDest");
        persister.persistModel(model, dest);
        executeAll(executor);

        Object storedModel = modelStore.tryLoadModel(dest);
        assertSame("model", model, storedModel);
    }

    @Test
    public void testPersistingOnExecutor() throws Exception {
        ManualTaskExecutor executor = new ManualTaskExecutor(true);
        final ContextAwareWrapper contextAwareExecutor = TaskExecutors.contextAware(executor);

        @SuppressWarnings("unchecked")
        final Consumer<Boolean> modelStorePersistModel = (Consumer<Boolean>)mock(Consumer.class);

        @SuppressWarnings("unchecked")
        PersistentModelStore<Object> modelStore = (PersistentModelStore<Object>)mock(PersistentModelStore.class);
        doAnswer((InvocationOnMock invocation) -> {
            modelStorePersistModel.accept(contextAwareExecutor.isExecutingInThis());
            return null;
        }).when(modelStore).persistModel(any(), any(Path.class));

        PersistentModelStore<Object> persister = createLazyStore(modelStore, contextAwareExecutor);

        persister.persistModel("MyModel", Paths.get("MyTestDest"));
        executeAll(executor);

        verify(modelStore).persistModel(any(), any(Path.class));
        verify(modelStorePersistModel).accept(eq(true));
    }

    @Test
    public void testGetQueuedModel() throws Exception {
        MemPersistentModelStore<Object> modelStore = new MemPersistentModelStore<>();
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        PersistentModelStore<Object> persister = createLazyStore(modelStore, executor);

        Path dest = Paths.get("MyTestDest");
        Object model = "MyModel";
        persister.persistModel(model, dest);
        assertSame("model", model, persister.tryLoadModel(dest));
    }

    @Test
    public void testOverwrite1() throws Exception {
        MemPersistentModelStore<Object> modelStore = new MemPersistentModelStore<>();
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        PersistentModelStore<Object> persister = createLazyStore(modelStore, executor);

        Object model1 = "MyModel1";
        Object model2 = "MyModel2";

        Path dest = Paths.get("MyTestDest");
        persister.persistModel(model1, dest);
        persister.persistModel(model2, dest);
        executeAll(executor);

        Object storedModel = modelStore.tryLoadModel(dest);
        assertSame("model", model2, storedModel);
    }

    @Test
    public void testOverwrite2() throws Exception {
        MemPersistentModelStore<Object> modelStore = new MemPersistentModelStore<>();
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        PersistentModelStore<Object> persister = createLazyStore(modelStore, executor);

        Object model1 = "MyModel1";
        Object model2 = "MyModel2";

        Path dest = Paths.get("MyTestDest");
        persister.persistModel(model1, dest);
        executeAll(executor);
        persister.persistModel(model2, dest);
        executeAll(executor);

        Object storedModel = modelStore.tryLoadModel(dest);
        assertSame("model", model2, storedModel);
    }

    @Test
    public void testStoreRetrievesFromWrapped() throws Exception {
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        Object model2 = "MyModel2";

        @SuppressWarnings("unchecked")
        PersistentModelStore<Object> modelStore = (PersistentModelStore<Object>)mock(PersistentModelStore.class);
        doReturn(model2).when(modelStore).tryLoadModel(any(Path.class));

        PersistentModelStore<Object> persister = createLazyStore(modelStore, executor);

        Object model1 = "MyModel1";

        Path dest = Paths.get("MyTestDest");
        persister.persistModel(model1, dest);
        executeAll(executor);

        Object storedModel = persister.tryLoadModel(dest);
        assertSame("model", model2, storedModel);
    }
}
