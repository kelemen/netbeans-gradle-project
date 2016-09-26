package org.netbeans.gradle.project.lookups;

import java.util.Collection;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.util.NbSupplier;
import org.openide.util.Lookup;

public final class LookupsEx {
    public static <T> NbSupplier<Collection<? extends T>> asSupplier(final Lookup src, final Class<? extends T> type) {
        ExceptionHelper.checkNotNullArgument(src, "src");
        ExceptionHelper.checkNotNullArgument(type, "type");

        return new NbSupplier<Collection<? extends T>>() {
            @Override
            public Collection<? extends T> get() {
                return src.lookupAll(type);
            }
        };
    }

    private LookupsEx() {
        throw new AssertionError();
    }
}
