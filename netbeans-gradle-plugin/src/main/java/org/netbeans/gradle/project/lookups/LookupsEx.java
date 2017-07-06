package org.netbeans.gradle.project.lookups;

import java.util.Collection;
import java.util.Objects;
import org.netbeans.gradle.project.util.NbSupplier;
import org.openide.util.Lookup;

public final class LookupsEx {
    public static <T> NbSupplier<Collection<? extends T>> asSupplier(final Lookup src, final Class<? extends T> type) {
        Objects.requireNonNull(src, "src");
        Objects.requireNonNull(type, "type");

        return () -> src.lookupAll(type);
    }

    private LookupsEx() {
        throw new AssertionError();
    }
}
