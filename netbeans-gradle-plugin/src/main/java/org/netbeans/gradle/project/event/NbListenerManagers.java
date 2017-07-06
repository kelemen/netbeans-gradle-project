package org.netbeans.gradle.project.event;

import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.event.ListenerRegistry;

public final class NbListenerManagers {
    public static <ListenerType> ListenerRegistry<ListenerType> neverNotifingRegistry() {
        // TODO: It is enough to have a single instance of this because of
        //       type erasure.
        return new ListenerRegistry<ListenerType>() {
            @Override
            public ListenerRef registerListener(ListenerType listener) {
                return ListenerRefs.unregistered();
            }

            @Override
            public int getListenerCount() {
                return 0;
            }
        };
    }

    private NbListenerManagers() {
        throw new AssertionError();
    }
}
