package org.netbeans.gradle.project;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class WaitableInterruptibleSignal {
    private final Lock lock;
    private final Condition signalEvent;
    private volatile boolean signaled;

    public WaitableInterruptibleSignal() {
        this.lock = new ReentrantLock();
        this.signalEvent = this.lock.newCondition();
        this.signaled = false;
    }

    public boolean isSignaled() {
        return signaled;
    }

    public void signal() {
        lock.lock();
        try {
            signaled = true;
            signalEvent.signalAll();
        } finally {
            lock.unlock();
        }
    }

    public boolean tryWaitForSignal() {
        if (signaled) {
            return true;
        }
        lock.lock();
        try {
            while (!signaled) {
                signalEvent.await();
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return signaled;
    }

    public boolean tryWaitForSignal(long timeout, TimeUnit unit) {
        if (signaled) {
            return true;
        }

        long remainingNanos = unit.toNanos(timeout);

        lock.lock();
        try {
            while (!signaled) {
                remainingNanos = signalEvent.awaitNanos(remainingNanos);
                if (remainingNanos <= 0) {
                    break;
                }
            }
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
        } finally {
            lock.unlock();
        }
        return signaled;
    }
}
