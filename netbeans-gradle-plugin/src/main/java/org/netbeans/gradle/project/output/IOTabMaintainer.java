package org.netbeans.gradle.project.output;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.netbeans.gradle.model.util.Exceptions;

public final class IOTabMaintainer<TabKey, IOTab extends IOTabDef> {
    private final Lock mainLock;
    private final IOTabFactory<? extends IOTab> factory;
    private final Map<TabKey, List<CountedTab<IOTab>>> currentTabs;
    private final KeyCounter<TabKey> tabIndexes;

    public IOTabMaintainer(IOTabFactory<? extends IOTab> factory) {
        this.mainLock = new ReentrantLock();
        this.currentTabs = new HashMap<>();
        this.factory = Objects.requireNonNull(factory, "factory");
        this.tabIndexes = new KeyCounter<>();
    }

    private CountedTab<IOTab> tryGetAvailable(TabKey key) {
        CountedTab<IOTab> result;

        do {
            mainLock.lock();
            try {
                List<CountedTab<IOTab>> list = currentTabs.get(key);
                if (list == null) {
                    return null;
                }

                result = null;
                for (CountedTab<IOTab> tab: list) {
                    if (result == null || tab.index < result.index) {
                        result = tab;
                    }
                }

                // Should never happen in the current implementation because
                // empty lists are removed and the list does not contain
                // null elements.
                if (result == null) {
                    return null;
                }

                list.remove(result);
                if (list.isEmpty()) {
                    currentTabs.remove(key);
                }
            } finally {
                mainLock.unlock();
            }
        } while (result.isClosed());

        return result;
    }

    private Set<CountedTab<IOTab>> getTabsToClose() {
        List<CountedTab<IOTab>> allTabs = new ArrayList<>();

        mainLock.lock();
        try {
            for (List<CountedTab<IOTab>> tabs: currentTabs.values()) {
                allTabs.addAll(tabs);
            }
        } finally {
            mainLock.unlock();
        }

        Set<CountedTab<IOTab>> result = new HashSet<>();
        for (CountedTab<IOTab> tab: allTabs) {
            if (tab.isClosed()) {
                result.add(tab);
            }
        }
        return result;
    }

    private void cleanupTabs() {
        Set<CountedTab<IOTab>> toClose = getTabsToClose();
        if (toClose.isEmpty()) {
            return;
        }

        mainLock.lock();
        try {
            for (List<CountedTab<IOTab>> tabs: currentTabs.values()) {
                Iterator<CountedTab<IOTab>> tabsItr = tabs.iterator();
                while (tabsItr.hasNext()) {
                    CountedTab<IOTab> tab = tabsItr.next();
                    if (toClose.contains(tab)) {
                        tabsItr.remove();
                    }
                }
            }

            Iterator<Map.Entry<TabKey, List<CountedTab<IOTab>>>> entryItr
                    = currentTabs.entrySet().iterator();

            while (entryItr.hasNext()) {
                Map.Entry<TabKey, List<CountedTab<IOTab>>> entry = entryItr.next();
                if (entry.getValue().isEmpty()) {
                    entryItr.remove();
                }
            }
        } finally {
            mainLock.unlock();
        }
    }

    private CountedTab<IOTab> newTabWithContext(TabKey key, String caption) {
        int index = tabIndexes.incAndGet(key);
        try {
            String captionWithIndex = index == 1
                    ? caption
                    : caption + " #" + index;
            IOTab tab = factory.create(captionWithIndex);
            return new CountedTab<>(index, tab);
        } catch (Throwable ex) {
            tabIndexes.decAndGet(key);
            throw Exceptions.throwUnchecked(ex);
        }
    }

    public IOTabRef<IOTab> getNewTab(TabKey key, String caption) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(caption, "caption");

        CountedTab<IOTab> result = newTabWithContext(key, caption);
        return new IOTabRefImpl(key, result);
    }

    public IOTabRef<IOTab> getTab(TabKey key, String caption) {
        Objects.requireNonNull(key, "key");
        Objects.requireNonNull(caption, "caption");

        cleanupTabs();

        CountedTab<IOTab> result = tryGetAvailable(key);
        if (result == null) {
            result = newTabWithContext(key, caption);
        }
        else {
            tabIndexes.incAndGet(key);
        }

        return new IOTabRefImpl(key, result);
    }

    private class IOTabRefImpl implements IOTabRef<IOTab> {
        private final TabKey key;
        private final CountedTab<IOTab> tab;
        private final AtomicBoolean closed;

        public IOTabRefImpl(TabKey key, CountedTab<IOTab> tab) {
            this.key = key;
            this.tab = tab;
            this.closed = new AtomicBoolean(false);
        }

        @Override
        public IOTab getTab() {
            return tab.tab;
        }

        @Override
        public void close() throws IOException {
            if (!closed.compareAndSet(false, true)) {
                return;
            }

            tabIndexes.decAndGet(key);

            mainLock.lock();
            try {
                List<CountedTab<IOTab>> tabList = currentTabs.get(key);
                if (tabList == null) {
                    tabList = new LinkedList<>();
                    currentTabs.put(key, tabList);
                }
                tabList.add(tab);
            } finally {
                mainLock.unlock();

                // Fixes memory leak: #256355 (netbeans.org/bugzilla)
                // Also, removes the boldness from the caption of the output tab.
                getTab().close();
            }
        }
    }

    private static final class CountedTab<IOTab extends IOTabDef> {
        public final int index;
        public final IOTab tab;

        public CountedTab(int index, IOTab tab) {
            this.index = index;
            this.tab = tab;
        }

        public boolean isClosed() {
            return tab.isDestroyed();
        }
    }
}
