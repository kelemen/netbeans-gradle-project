package org.netbeans.gradle.project.util;

import java.io.File;
import java.net.URL;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.model.util.ConstructableWeakRef;
import org.netbeans.gradle.model.util.NbSupplier5;
import org.openide.filesystems.FileUtil;

public final class UrlFactory {
    private static final NbSupplier5<UrlFactory> DEFAULT_REF = new ConstructableWeakRef<>(new NbSupplier5<UrlFactory>() {
        @Override
        public UrlFactory get() {
            return new UrlFactory();
        }
    });

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
        return DEFAULT_REF.get();
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
