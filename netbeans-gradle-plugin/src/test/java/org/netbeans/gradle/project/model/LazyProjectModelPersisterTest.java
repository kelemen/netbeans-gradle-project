package org.netbeans.gradle.project.model;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.jtrim.concurrent.ContextAwareWrapper;
import org.jtrim.concurrent.ManualTaskExecutor;
import org.jtrim.concurrent.TaskExecutors;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.netbeans.gradle.project.util.NbConsumer;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class LazyProjectModelPersisterTest {
    private static void executeAll(ManualTaskExecutor executor) {
        while (executor.executeCurrentlySubmitted() > 0) {
            // One more time to execute tasks submitted by the executed tasks.
        }
    }

    @Test
    public void testPersistOne() throws Exception {
        MemPersistentModelStore<Object> modelStore = new MemPersistentModelStore<>();
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        LazyPersistentModelStore<Object> persister = new LazyPersistentModelStore<>(modelStore, executor);

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
        final NbConsumer<Boolean> modelStorePersistModel = (NbConsumer<Boolean>)mock(NbConsumer.class);

        @SuppressWarnings("unchecked")
        PersistentModelStore<Object> modelStore = (PersistentModelStore<Object>)mock(PersistentModelStore.class);
        doAnswer(new Answer<Object>() {
            @Override
            public Object answer(InvocationOnMock invocation) throws Throwable {
                modelStorePersistModel.accept(contextAwareExecutor.isExecutingInThis());
                return null;
            }
        }).when(modelStore).persistModel(any(), any(Path.class));

        LazyPersistentModelStore<Object> persister = new LazyPersistentModelStore<>(modelStore, contextAwareExecutor);

        persister.persistModel("MyModel", Paths.get("MyTestDest"));
        executeAll(executor);

        verify(modelStore).persistModel(any(), any(Path.class));
        verify(modelStorePersistModel).accept(eq(true));
    }

    @Test
    public void testGetQueuedModel() throws Exception {
        MemPersistentModelStore<Object> modelStore = new MemPersistentModelStore<>();
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        LazyPersistentModelStore<Object> persister = new LazyPersistentModelStore<>(modelStore, executor);

        Path dest = Paths.get("MyTestDest");
        Object model = "MyModel";
        persister.persistModel(model, dest);
        assertSame("model", model, persister.tryLoadModel(dest));
    }

    @Test
    public void testOverwrite1() throws Exception {
        MemPersistentModelStore<Object> modelStore = new MemPersistentModelStore<>();
        ManualTaskExecutor executor = new ManualTaskExecutor(true);

        LazyPersistentModelStore<Object> persister = new LazyPersistentModelStore<>(modelStore, executor);

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

        LazyPersistentModelStore<Object> persister = new LazyPersistentModelStore<>(modelStore, executor);

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
    public void testTryLoadModel() throws Exception {
    }
}
