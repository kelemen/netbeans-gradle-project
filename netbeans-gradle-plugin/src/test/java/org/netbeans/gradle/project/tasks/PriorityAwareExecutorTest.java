package org.netbeans.gradle.project.tasks;

import java.util.function.BiConsumer;
import org.jtrim2.cancel.Cancellation;
import org.jtrim2.cancel.CancellationSource;
import org.jtrim2.cancel.CancellationToken;
import org.jtrim2.cancel.OperationCanceledException;
import org.jtrim2.executor.CancelableTask;
import org.jtrim2.executor.ManualTaskExecutor;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class PriorityAwareExecutorTest {
    @Test
    public void testWithoutCleanup() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        PriorityAwareExecutor executor = new PriorityAwareExecutor(wrapped);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        executor.getLowPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task2);
        executor.getHighPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task1);

        wrapped.executeCurrentlySubmitted();

        InOrder inOrder = inOrder(task1, task2);

        inOrder.verify(task1).execute(any(CancellationToken.class));
        inOrder.verify(task2).execute(any(CancellationToken.class));
    }

    @SuppressWarnings("unchecked")
    private static <R> BiConsumer<R, Throwable> mockCleanup() {
        return (BiConsumer<R, Throwable>)mock(BiConsumer.class);
    }

    @Test
    public void testWithCleanup() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        PriorityAwareExecutor executor = new PriorityAwareExecutor(wrapped);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        BiConsumer<Void, Throwable> cleanup1 = mockCleanup();
        BiConsumer<Void, Throwable> cleanup2 = mockCleanup();

        executor.getLowPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task2)
                .whenComplete(cleanup2);
        executor.getHighPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task1)
                .whenComplete(cleanup1);

        wrapped.executeCurrentlySubmitted();

        InOrder inOrder = inOrder(task1, task2, cleanup1, cleanup2);

        inOrder.verify(task1).execute(any(CancellationToken.class));
        inOrder.verify(cleanup1).accept(null, null);
        inOrder.verify(task2).execute(any(CancellationToken.class));
        inOrder.verify(cleanup2).accept(null, null);
    }

    @Test
    public void testCanceledWithoutCleanup() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        PriorityAwareExecutor executor = new PriorityAwareExecutor(wrapped);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        CancellationSource cancel1 = Cancellation.createCancellationSource();

        executor.getLowPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task2);
        executor.getHighPriorityExecutor().execute(cancel1.getToken(), task1);

        cancel1.getController().cancel();

        wrapped.executeCurrentlySubmitted();

        verifyZeroInteractions(task1);
        verify(task2).execute(any(CancellationToken.class));
    }

    @Test
    public void testCanceledWithCleanup() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        PriorityAwareExecutor executor = new PriorityAwareExecutor(wrapped);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        BiConsumer<Void, Throwable> cleanup1 = mockCleanup();
        BiConsumer<Void, Throwable> cleanup2 = mockCleanup();

        CancellationSource cancel1 = Cancellation.createCancellationSource();

        executor.getLowPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task2)
                .whenComplete(cleanup2);
        executor.getHighPriorityExecutor().execute(cancel1.getToken(), task1)
                .whenComplete(cleanup1);

        cancel1.getController().cancel();

        wrapped.executeCurrentlySubmitted();

        InOrder inOrder = inOrder(task2, cleanup1, cleanup2);

        verifyZeroInteractions(task1);

        inOrder.verify(cleanup1).accept(isNull(null), isA(OperationCanceledException.class));
        inOrder.verify(task2).execute(any(CancellationToken.class));
        inOrder.verify(cleanup2).accept(isNull(null), isNull(Throwable.class));
    }
}
