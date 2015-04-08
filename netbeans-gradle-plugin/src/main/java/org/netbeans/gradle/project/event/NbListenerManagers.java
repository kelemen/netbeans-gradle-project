package org.netbeans.gradle.project.event;

import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistry;
import org.jtrim.event.UnregisteredListenerRef;

public final class NbListenerManagers {
    public static <ListenerType> ListenerRegistry<ListenerType> neverNotifingRegistry() {
        // TODO: It is enough to have a single instance of this because of
        //       type erasure.
        return new ListenerRegistry<ListenerType>() {
            @Override
            public ListenerRef registerListener(ListenerType listener) {
                return UnregisteredListenerRef.INSTANCE;
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
