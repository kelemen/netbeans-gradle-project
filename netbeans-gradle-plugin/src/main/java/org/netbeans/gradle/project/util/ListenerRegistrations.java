package org.netbeans.gradle.project.util;

import java.util.ArrayList;
import java.util.List;
import org.jtrim2.event.ListenerRef;

public final class ListenerRegistrations {
    private final List<ListenerRef> listenerRefs;

    public ListenerRegistrations() {
        this.listenerRefs = new ArrayList<>();
    }

    public void add(ListenerRef listenerRef) {
        listenerRefs.add(listenerRef);
    }

    public void unregisterAll() {
        for (ListenerRef listenerRef: listenerRefs) {
            listenerRef.unregister();
        }
        listenerRefs.clear();
    }
}
