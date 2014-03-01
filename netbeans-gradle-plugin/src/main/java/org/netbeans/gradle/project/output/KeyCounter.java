package org.netbeans.gradle.project.output;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class KeyCounter<Key> {
    private final Lock mainLock;
    private final Map<Key, Counter> keys;

    public KeyCounter() {
        this.mainLock = new ReentrantLock();
        this.keys = new HashMap<>();
    }

    public int getCount(Key key) {
        mainLock.lock();
        try {
            Counter counter = keys.get(key);
            return counter != null ? counter.count : 0;
        } finally {
            mainLock.unlock();
        }
    }

    public int addAndGet(Key key, int amount) {
        mainLock.lock();
        try {
            Counter counter = keys.get(key);
            if (counter == null) {
                keys.put(key, new Counter(amount));
                return amount;
            }
            else {
                int result = counter.count + amount;

                if (result == 0) {
                    keys.remove(key);
                }
                else {
                    counter.count = result;
                }

                return result;
            }
        } finally {
            mainLock.unlock();
        }
    }

    public int incAndGet(Key key) {
        return addAndGet(key, 1);
    }

    public int decAndGet(Key key) {
        return addAndGet(key, -1);
    }

    private static final class Counter {
        public int count;

        public Counter(int count) {
            this.count = count;
        }
    }
}
