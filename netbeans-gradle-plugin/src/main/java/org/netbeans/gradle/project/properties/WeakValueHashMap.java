package org.netbeans.gradle.project.properties;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.AbstractCollection;
import java.util.AbstractMap;
import java.util.AbstractSet;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Set;

public final class WeakValueHashMap<K, V> extends AbstractMap<K, V> {
    // Note that this is not a true general purpose implementation.
    // This class need to be rewritten if need to be used where performance matters,
    // also the view of this map (values() and entrySet()) are read-only and
    // inefficient.

    private final Map<K, TableRef<K, V>> wrappedMap;
    private final ReferenceQueue<V> references;

    public WeakValueHashMap() {
        this.wrappedMap = new HashMap<>();
        this.references = new ReferenceQueue<>();
    }

    private void removeUnreferenced() {
        while (true) {
            @SuppressWarnings("unchecked")
            TableRef<K, V> ref = (TableRef<K, V>)references.poll();
            if (ref == null) {
                break;
            }
            wrappedMap.remove(ref.getKey());
        }
    }

    private Map<K, TableRef<K, V>> getMap() {
        removeUnreferenced();
        return wrappedMap;
    }

    @Override
    public int size() {
        if (wrappedMap.isEmpty()) {
            return 0;
        }

        return getMap().size();
    }

    @Override
    public boolean isEmpty() {
        return size() == 0;
    }

    @Override
    public boolean containsKey(Object key) {
        Objects.requireNonNull(key, "key");
        return getMap().containsKey(key);
    }

    @Override
    public boolean containsValue(Object value) {
        Objects.requireNonNull(value, "value");

        Map<K, TableRef<K, V>> map = getMap();
        for (TableRef<?, V> valueRef: map.values()) {
            V currentValue = valueRef.getValue();
            if (value.equals(currentValue)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public V get(Object key) {
        Objects.requireNonNull(key, "key");

        WeakReference<V> resultRef = getMap().get(key);
        return resultRef != null ? resultRef.get() : null;
    }

    @Override
    public V put(K key, V value) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(value, "value");

        WeakReference<V> resultRef = getMap().put(key, new TableRef<>(key, value, references));
        return resultRef != null ? resultRef.get() : null;
    }

    @Override
    public V remove(Object key) {
        Objects.requireNonNull(key, "key");

        WeakReference<V> resultRef = getMap().remove(key);
        return resultRef != null ? resultRef.get() : null;
    }

    @Override
    public void clear() {
        wrappedMap.clear();
        removeUnreferenced();
    }

    @Override
    public Set<K> keySet() {
        return getMap().keySet();
    }

    @Override
    public Collection<V> values() {
        return new AbstractCollection<V>() {
            @Override
            public Iterator<V> iterator() {
                return new WeakRefItr<>(getMap().values().iterator());
            }

            @Override
            public int size() {
                return WeakValueHashMap.this.size();
            }
        };
    }

    @Override
    public Set<Entry<K, V>> entrySet() {
        return new AbstractSet<Entry<K, V>>() {
            @Override
            public Iterator<Entry<K, V>> iterator() {
                return new EntryItr<>(getMap().entrySet().iterator());
            }

            @Override
            public int size() {
                return WeakValueHashMap.this.size();
            }
        };
    }

    private static class EntryItr<K, V> implements Iterator<Entry<K, V>> {
        private final Iterator<Entry<K, TableRef<K, V>>> itr;
        private Entry<K, V> nextValue;

        public EntryItr(Iterator<Entry<K, TableRef<K, V>>> itr) {
            assert itr != null;
            this.itr = itr;
            moveToNext();
        }

        private void moveToNext() {
            while (itr.hasNext()) {
                Entry<K, TableRef<K, V>> next = itr.next();
                V nextEntryValue = next.getValue().getValue();
                if (nextEntryValue != null) {
                    nextValue = new AbstractMap.SimpleImmutableEntry<>(
                            next.getKey(), nextEntryValue);
                    return;
                }
            }
            nextValue = null;
        }

        @Override
        public boolean hasNext() {
            return nextValue != null;
        }

        @Override
        public Entry<K, V> next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            Entry<K, V> result = nextValue;
            moveToNext();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("The view of the entryset of this map is read-only.");
        }
    }

    private static class WeakRefItr<K, V> implements Iterator<V> {
        private final Iterator<TableRef<K, V>> itr;
        private V nextValue;

        public WeakRefItr(Iterator<TableRef<K, V>> itr) {
            assert itr != null;
            this.itr = itr;
            moveToNext();
        }

        private void moveToNext() {
            while (itr.hasNext()) {
                TableRef<?, V> next = itr.next();
                nextValue = next.getValue();
                if (nextValue != null) {
                    return;
                }
            }
            nextValue = null;
        }

        @Override
        public boolean hasNext() {
            return nextValue != null;
        }

        @Override
        public V next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            V result = nextValue;
            moveToNext();
            return result;
        }

        @Override
        public void remove() {
            throw new UnsupportedOperationException("The view of the values of this map is read-only.");
        }
    }

    private static class TableRef<K, V> extends WeakReference<V> {
        private final K key;

        public TableRef(K key, V value, ReferenceQueue<V> queue) {
            super(value, queue);

            this.key = key;
        }

        public K getKey() {
            return key;
        }

        public V getValue() {
            return super.get();
        }
    }
}
