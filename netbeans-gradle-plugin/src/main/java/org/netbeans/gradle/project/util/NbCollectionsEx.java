package org.netbeans.gradle.project.util;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

public final class NbCollectionsEx {
    private static final Object[] EMPTY_ARRAY = new Object[0];
    private static final Collection<?> DEV_NULL_COLLECTION = new DevNullCollection<>();

    @SuppressWarnings("unchecked")
    public static <E> Collection<E> getDevNullCollection() {
        return (Collection<E>)DEV_NULL_COLLECTION;
    }

    private static class DevNullCollection<T> implements Collection<T> {
        @Override
        public int size() {
            return 0;
        }

        @Override
        public boolean isEmpty() {
            return true;
        }

        @Override
        public boolean contains(Object o) {
            return false;
        }

        @Override
        public Iterator<T> iterator() {
            return Collections.emptyIterator();
        }

        @Override
        public Object[] toArray() {
            return EMPTY_ARRAY;
        }

        @Override
        public <T> T[] toArray(T[] a) {
            if (a.length > 0) {
                a[0] = null;
            }
            return a;
        }

        @Override
        public boolean add(T e) {
            return false;
        }

        @Override
        public boolean remove(Object o) {
            return false;
        }

        @Override
        public boolean containsAll(Collection<?> c) {
            return c.isEmpty();
        }

        @Override
        public boolean addAll(Collection<? extends T> c) {
            return false;
        }

        @Override
        public boolean removeAll(Collection<?> c) {
            return false;
        }

        @Override
        public boolean retainAll(Collection<?> c) {
            return false;
        }

        @Override
        public void clear() {
        }
    }

    private NbCollectionsEx() {
        throw new AssertionError();
    }
}
