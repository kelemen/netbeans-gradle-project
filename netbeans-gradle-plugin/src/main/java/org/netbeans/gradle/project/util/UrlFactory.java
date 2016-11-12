package org.netbeans.gradle.project.util;

import java.io.File;
import java.lang.ref.WeakReference;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicReference;
import org.jtrim.utils.ExceptionHelper;
import org.openide.filesystems.FileUtil;

public final class UrlFactory {
    private static final AtomicReference<WeakReference<UrlFactory>> DEFAULT_REF_REF = new AtomicReference<>();

    private final NbFunction<? super File, ? extends URL> urlCreator;
    private final ConcurrentMap<File, URL> cache;

    public UrlFactory() {
        this(new NbFunction<File, URL>() {
            @Override
            public URL apply(File entry) {
                return FileUtil.urlForArchiveOrDir(entry);
            }
        });
    }

    public UrlFactory(NbFunction<? super File, ? extends URL> urlCreator) {
        ExceptionHelper.checkNotNullArgument(urlCreator, "urlCreator");

        this.urlCreator = urlCreator;
        this.cache = new ConcurrentHashMap<>(256);
    }

    public static URL urlForArchiveOrDir(File entry) {
        return getDefaultArchiveOrDirFactory().toUrl(entry);
    }

    public static UrlFactory getDefaultArchiveOrDirFactory() {
        while (true) {
            WeakReference<UrlFactory> defaultRef = DEFAULT_REF_REF.get();
            UrlFactory defaultFactory = defaultRef != null ? defaultRef.get() : null;

            if (defaultFactory == null) {
                defaultFactory = createDefault();
                if (!DEFAULT_REF_REF.compareAndSet(defaultRef, new WeakReference<>(defaultFactory))) {
                    continue;
                }
            }
            return defaultFactory;
        }
    }

    private static UrlFactory createDefault() {
        return new UrlFactory();
    }

    public URL toUrl(File entry) {
        URL result = cache.get(entry);
        if (result != null) {
            return result;
        }

        result = urlCreator.apply(entry);
        if (result == null) {
            return null;
        }

        URL prevValue = cache.putIfAbsent(entry, result);
        return prevValue != null ? prevValue : result;
    }
}
