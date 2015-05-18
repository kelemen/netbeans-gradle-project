package org.netbeans.gradle.project.tasks;

import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationSource;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.concurrent.CleanupTask;
import org.jtrim.concurrent.ManualTaskExecutor;
import org.junit.Test;
import org.mockito.InOrder;

import static org.mockito.Mockito.*;

public class PriorityAwareExecutorTest {
    @Test
    public void testWithoutCleanup() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        PriorityAwareExecutor executor = new PriorityAwareExecutor(wrapped);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        executor.getLowPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task2, null);
        executor.getHighPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task1, null);

        wrapped.executeCurrentlySubmitted();

        InOrder inOrder = inOrder(task1, task2);

        inOrder.verify(task1).execute(any(CancellationToken.class));
        inOrder.verify(task2).execute(any(CancellationToken.class));
    }

    @Test
    public void testWithCleanup() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        PriorityAwareExecutor executor = new PriorityAwareExecutor(wrapped);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        CleanupTask cleanup1 = mock(CleanupTask.class);
        CleanupTask cleanup2 = mock(CleanupTask.class);

        executor.getLowPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task2, cleanup2);
        executor.getHighPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task1, cleanup1);

        wrapped.executeCurrentlySubmitted();

        InOrder inOrder = inOrder(task1, task2, cleanup1, cleanup2);

        inOrder.verify(task1).execute(any(CancellationToken.class));
        inOrder.verify(cleanup1).cleanup(eq(false), isNull(Throwable.class));
        inOrder.verify(task2).execute(any(CancellationToken.class));
        inOrder.verify(cleanup2).cleanup(eq(false), isNull(Throwable.class));
    }

    @Test
    public void testCanceledWithoutCleanup() throws Exception {
        ManualTaskExecutor wrapped = new ManualTaskExecutor(false);
        PriorityAwareExecutor executor = new PriorityAwareExecutor(wrapped);

        CancelableTask task1 = mock(CancelableTask.class);
        CancelableTask task2 = mock(CancelableTask.class);

        CancellationSource cancel1 = Cancellation.createCancellationSource();

        executor.getLowPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task2, null);
        executor.getHighPriorityExecutor().execute(cancel1.getToken(), task1, null);

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

        CleanupTask cleanup1 = mock(CleanupTask.class);
        CleanupTask cleanup2 = mock(CleanupTask.class);

        CancellationSource cancel1 = Cancellation.createCancellationSource();

        executor.getLowPriorityExecutor().execute(Cancellation.UNCANCELABLE_TOKEN, task2, cleanup2);
        executor.getHighPriorityExecutor().execute(cancel1.getToken(), task1, cleanup1);

        cancel1.getController().cancel();

        wrapped.executeCurrentlySubmitted();

        InOrder inOrder = inOrder(task2, cleanup1, cleanup2);

        verifyZeroInteractions(task1);

        inOrder.verify(cleanup1).cleanup(eq(true), isNull(Throwable.class));
        inOrder.verify(task2).execute(any(CancellationToken.class));
        inOrder.verify(cleanup2).cleanup(eq(false), isNull(Throwable.class));
    }
}
