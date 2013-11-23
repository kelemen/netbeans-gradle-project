package org.netbeans.gradle.model.util;

import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Defines a simple safety-net for objects managing unmanaged resources (e.g.:
 * files).
 * <P>
 * {@code ObjectFinalizer} takes a {@link Runnable} in its constructor and
 * allows it to be called in the {@link #doFinalize() doFinalize()} method.
 * Also, {@code ObjectFinalizer} declares a {@link Object#finalize() finalizer},
 * in which it will call {@code run()} method of the specified {@code Runnable}
 * if it was not called manually (by calling {@code doFinalize()}).
 * <P>
 * Once the {@code doFinalize()} method of an {@code ObjectFinalizer} was called
 * it will no longer retain a reference to the {@code Runnable} specified at
 * construction time.
 * <P>
 * See the following example code using {@code ObjectFinalizer}:
 * <code><pre>
 * class UnmanagedResourceHolder implements Closable {
 *   privata final ObjectFinalizer finalizer;
 *
 *   // Other declarations ...
 *
 *   public UnmanagedResourceHolder() {
 *     // Initialization code ...
 *
 *     this.finalizer = new ObjectFinalizer(new Runnable() {
 *       public void run() {
 *         doCleanup();
 *       }
 *     }, "UnmanagedResourceHolder.cleanup");
 *   }
 *
 *   // Other code ...
 *
 *   private void doCleanup() {
 *     // cleanup unmanaged resources
 *   }
 *
 *   {@literal @Override}
 *   public void close() {
 *     finalizer.doFinalize();
 *   }
 * }
 * </pre></code>
 * <P>
 * Assume, that in the above code an {@code UnmanagedResourceHolder} instance
 * becomes unreachable and as such is eligible for garbage collection. Notice
 * that in this case {@code finalizer} also becomes unreachable and such also
 * eligible for garbage collection. Also assume that the {@code close()} method
 * was not called. In this case when the JVM decides to cleanup the now
 * unreachable {@code finalizer} instance, it will call its finalizer and in
 * turn invoke the {@code doCleanup()} method releasing the unmanaged resources.
 *
 * <h3>Unmanaged Resources</h3>
 * In Java a garbage collector is employed, so the programmer is relieved from
 * the burden (mostly) of manual memory management. However, the garbage
 * collector cannot handle anything beyond the memory allocated for objects, so
 * other unmanaged resources require a cleanup method. Although there are
 * {@link Object#finalize() finalizers} in Java, they are unreliable and the
 * only correct solution is the use of a cleanup method. Generally, objects
 * managing unmanaged resources should implement the {@link AutoCloseable}
 * interface.
 * <P>
 * Notice however, that a bug may prevent the program to call the cleanup
 * method of an object, causing the leakage of unmanaged resource, possibly
 * leading to resource exhaustion. In this case finalizers can be useful to
 * provide a safety-net, that even in the case of such previously mentioned bug,
 * a finalizer can be implemented to do the cleanup, so when the JVM actually
 * removes the object, it may cleanup the unmanaged resources.
 *
 * <h3>Benefits of {@code ObjectFinalizer}</h3>
 * One may ask what are the benefits of using {@code ObjectFinalizer} instead of
 * directly declaring a finalizer. There are actually three main benefits of
 * using {@code ObjectFinalizer}:
 * <P>
 * There is a penalty for declaring a {@code finalize()} method in a class: Such
 * objects are slower to be created and will be garbage collected later. There
 * is nothing to be done about slower object creation but the use of
 * {@code ObjectFinalizer} will help with the slower garbage collection.
 * It is assumed that the common case is that the {@code doFinalize()} method is
 * called from the code correctly without relying on the object finalizer
 * (ideally this should always be the case). In this case once the
 * {@code doFinalize()} method was called, the {@code ObjectFinalizer} will
 * store no reference to the object it cleaned up, allowing this object to be
 * garbage collected. Note that the {@code ObjectFinalizer} instance will still
 * suffer from the slower garbage collection but an {@code ObjectFinalizer}
 * whose {@code doFinalize()} method was called is relatively cheap in terms of
 * retained memory. So this can be a considerable benefit if the object
 * protected by the {@code ObjectFinalizer} retains considerable amount of
 * memory.
 * <P>
 * Notice that the {@code run()} method of the specified {@code Runnable}
 * instance can only be called at most once even if the {@code doFinalize()}
 * method is called concurrently multiple times. This effectively makes the
 * cleanup method idempotent for free which makes the cleanup method safer.
 * <P>
 * In case the cleanup method was failed to be called, the
 * {@code ObjectFinalizer} will log this failure when it detects in its
 * finalizer, that the {@code doFinalize()} method was not called. So this error
 * will be documented in the logs and can be analyzed later.
 *
 * <h3>Thread safety</h3>
 * This class is safe to be used by multiple threads concurrently.
 *
 * <h4>Synchronization transparency</h4>
 * Unless otherwise noted, methods of this class are not
 * <I>synchronization transparent</I>.
 */
public final class ObjectFinalizer {
    private static final String MISSED_FINALIZE_MESSAGE
            = "An object was not finalized explicitly."
            + " Finalizer task: {0}/{1}.";

    private static final Logger LOGGER = Logger.getLogger(ObjectFinalizer.class.getName());

    private final AtomicReference<Runnable> finalizerTask;
    private final String className;
    private final String taskDescription;

    /**
     * Creates a new {@code ObjectFinalizer} using the specified
     * {@code Runnable} to be called by the {@link #doFinalize() doFinalize()}
     * method.
     * <P>
     * The task description to be used in the log when {@code doFinalize()}
     * is failed to get called is the result of the {@code toString()}
     * method of the specified task. The result of the {@code toString()} is
     * retrieved in this constructor call and not when actually required.
     *
     * @param finalizerTask the task to be invoked by the {@code doFinalize()}
     *   method to cleanup unmanaged resources. This argument cannot be
     *   {@code null}.
     *
     * @throws NullPointerException thrown if the specified task is
     *   {@code null} or its {@code toString()} method returns {@code null}
     */
    public ObjectFinalizer(Runnable finalizerTask) {
        this(finalizerTask, finalizerTask.toString());
    }

    /**
     * Creates a new {@code ObjectFinalizer} using the specified
     * {@code Runnable} to be called by the {@link #doFinalize() doFinalize()}
     * method and a task description to be used in logs if {@code doFinalize()}
     * is only called in the finalizer.
     *
     * @param finalizerTask the task to be invoked by the {@code doFinalize()}
     *   method to cleanup unmanaged resources. This argument cannot be
     *   {@code null}.
     * @param taskDescription the description to be added to the log message
     *   if the {@code doFinalize()} only gets called in the finalizer
     *
     * @throws NullPointerException thrown if the specified task or the task
     *   description is {@code null}
     */
    public ObjectFinalizer(Runnable finalizerTask, String taskDescription) {
        if (finalizerTask == null) throw new NullPointerException("finalizerTask");
        if (taskDescription == null) throw new NullPointerException("taskDescription");

        this.finalizerTask = new AtomicReference<Runnable>(finalizerTask);
        this.taskDescription = taskDescription;
        this.className = finalizerTask.getClass().getName();
    }

    /**
     * Sets the state as if {@link #doFinalize() doFinalize()} has been called
     * but does not actually call {@code doFinalize}. This method is useful if
     * the object has been finalized in another way, so it is no longer an error
     * not to finalize the object.
     * <P>
     * After calling this method, subsequent {@code doFinalize()} method calls
     * will do nothing.
     */
    public void markFinalized() {
        finalizerTask.set(null);
    }

    /**
     * Invokes the task specified at construction time if it was not called yet.
     * The task will be called only once, even if this method is called
     * concurrently by multiple threads.
     * <P>
     * The task is invoked synchronously on the current calling thread. Note
     * that therefore, this method will propagate every exception to the caller
     * thrown by the task.
     * <P>
     * Once this method returns (even if the called task throws an exception),
     * the task specified at construction time will no longer be referenced by
     * this {@code ObjectFinalizer}.
     *
     * @return {@code true} if this method actually invoked the task specified
     *   at construction time, {@code false} if {@code doFinalize()} was already
     *   called (or another {@code doFinalize()} is executing the task
     *   concurrently)
     */
    public boolean doFinalize() {
        Runnable task = finalizerTask.getAndSet(null);
        if (task != null) {
            task.run();
            return true;
        }

        return false;
    }

    /**
     * Returns {@code true} if {@link #doFinalize() doFinalize()} has already
     * been called. In case this method returns {@code true}, subsequent calls
     * to {@code doFinalize()} will do nothing but return immediately to the
     * caller.
     *
     * @return {@code true} if {@code doFinalize()} has already
     *   been called, {@code false} otherwise
     */
    public boolean isFinalized() {
        return finalizerTask.get() == null;
    }

    /**
     * Throws an {@link IllegalStateException} if the
     * {@link #doFinalize() doFinalize()} method has already been called. This
     * method can be used to implement a fail-fast behaviour when the object
     * this {@code ObjectFinalizer} protects is being used after cleanup.
     *
     * @throws IllegalStateException thrown if {@code doFinalize()} has already
     *   been called. That is, if {@link #isFinalized() isFinalized()} returns
     *   {@code true}.
     */
    public void checkNotFinalized() {
        if (isFinalized()) {
            throw new IllegalStateException("Object was already finalized: "
                    + className + "/" + taskDescription);
        }
    }

    /**
     * Invokes the task specified at construction time if it has not been called
     * yet. In case the task has already been called, this method does nothing
     * but returns immediately.
     * <P>
     * If the task has not been called previously (which is considered to be an
     * error), this method will log a {@link Level#SEVERE SEVERE} level log
     * message using the {@code java.util.logging} facility. The logging will be
     * done even if the task throws an exception in which case this exception
     * will also be attached to the log message.
     */
    @Override
    @SuppressWarnings("FinalizeDoesntCallSuperFinalize")
    protected void finalize() {
        Throwable exception = null;
        Runnable task = null;

        try {
            task = finalizerTask.getAndSet(null);
            if (task != null) {
                task.run();
            }
        } catch (Throwable ex) {
            exception = ex;
        }

        if (task != null && LOGGER.isLoggable(Level.SEVERE)) {
            LogRecord logRecord
                    = new LogRecord(Level.SEVERE, MISSED_FINALIZE_MESSAGE);

            logRecord.setSourceClassName(ObjectFinalizer.class.getName());
            logRecord.setSourceMethodName("finalize()");
            logRecord.setThrown(exception);
            logRecord.setParameters(new Object[]{className, taskDescription});

            LOGGER.log(logRecord);
        }
    }
}
