package org.netbeans.gradle.project.properties;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import org.jtrim.concurrent.Tasks;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.util.NbConsumer;
import org.netbeans.gradle.project.util.TestDetectUtils;

public final class ProfileSettingsContainer {
    private static final AtomicReference<ProfileSettingsContainer> DEFAULT_REF = new AtomicReference<>(null);

    private final Lock mainLock;
    private final WeakValueHashMap<ProfileSettingsKey, LoadableSingleProfileSettingsEx> loaded;

    private ProfileSettingsContainer() {
        this.mainLock = new ReentrantLock();
        this.loaded = new WeakValueHashMap<>();
    }

    public static ProfileSettingsContainer getDefault() {
        ProfileSettingsContainer result = DEFAULT_REF.get();
        if (result == null) {
            result = new ProfileSettingsContainer();
            if (DEFAULT_REF.compareAndSet(null, result)) {
                if (!TestDetectUtils.isRunningTests()) {
                    // We must not add this shutdown hook when running tests because
                    // it would cause a dead-lock in NetBeans.
                    Runtime.getRuntime().addShutdownHook(new Thread(result::saveAllProfilesNow));
                }
            }
            else {
                result = DEFAULT_REF.get();
            }
        }
        return result;
    }

    private void saveAllProfilesNow() {
        List<LoadableSingleProfileSettingsEx> toSave;
        mainLock.lock();
        try {
            toSave = new ArrayList<>(loaded.values());
        } finally {
            mainLock.unlock();
        }

        for (LoadableSingleProfileSettingsEx settings: toSave) {
            settings.saveAndWait();
        }
    }

    private LoadableSingleProfileSettingsEx getUnloadedProfileSettings(ProfileSettingsKey key) {
        ExceptionHelper.checkNotNullArgument(key, "key");

        LoadableSingleProfileSettingsEx result;

        mainLock.lock();
        try {
            result = loaded.get(key);
            if (result == null) {
                result = key.openUnloadedProfileSettings();
                loaded.put(key, result);
            }
        } finally {
            mainLock.unlock();
        }

        return result;
    }

    public SingleProfileSettingsEx loadProfileSettings(ProfileSettingsKey key) {
        LoadableSingleProfileSettingsEx result = getUnloadedProfileSettings(key);
        result.ensureLoadedAndWait();
        return result;
    }

    public ListenerRef loadProfileSettings(
            ProfileSettingsKey key,
            final NbConsumer<? super SingleProfileSettingsEx> listener) {
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        LoadableSingleProfileSettingsEx result = getUnloadedProfileSettings(key);
        result.ensureLoaded();
        return result.notifyWhenLoaded(() -> {
            listener.accept(result);
        });
    }

    public ListenerRef loadAllProfileSettings(
            Collection<ProfileSettingsKey> keys,
            final NbConsumer<? super List<SingleProfileSettingsEx>> listener) {
        ExceptionHelper.checkNotNullElements(keys, "keys");
        ExceptionHelper.checkNotNullArgument(listener, "listener");

        final List<LoadableSingleProfileSettingsEx> result = new ArrayList<>(keys.size());
        for (ProfileSettingsKey key: keys) {
            result.add(getUnloadedProfileSettings(key));
        }

        List<ListenerRef> resultRefs = new ArrayList<>(result.size());

        final AtomicInteger loadCount = new AtomicInteger(result.size());
        for (LoadableSingleProfileSettingsEx settings: result) {
            settings.ensureLoaded();
            ListenerRef notifyRef = settings.notifyWhenLoaded(Tasks.runOnceTask(() -> {
                if (loadCount.decrementAndGet() == 0) {
                    listener.accept(new ArrayList<>(result));
                }
            }, false));
            resultRefs.add(notifyRef);
        }

        return ListenerRegistries.combineListenerRefs(resultRefs);
    }

    public List<SingleProfileSettingsEx> loadAllProfileSettings(Collection<ProfileSettingsKey> keys) {
        ExceptionHelper.checkNotNullElements(keys, "keys");

        List<SingleProfileSettingsEx> result = new ArrayList<>(keys.size());
        for (ProfileSettingsKey key: keys) {
            result.add(loadProfileSettings(key));
        }
        return result;
    }
}
