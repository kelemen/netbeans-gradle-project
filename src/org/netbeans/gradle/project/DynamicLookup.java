package org.netbeans.gradle.project;

import java.util.Collection;
import java.util.List;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

public final class DynamicLookup extends ProxyLookup {
    public DynamicLookup(Lookup... lookups) {
        super(lookups);
    }

    public DynamicLookup() {
    }

    public void replaceLookups(Lookup... lookups) {
        super.setLookups(lookups);
    }

    public void replaceLookups(List<Lookup> lookups) {
        super.setLookups(lookups.toArray(new Lookup[lookups.size()]));
    }

    public static Lookup viewLookup(final Lookup lookup) {
        if (lookup == null) throw new NullPointerException("lookup");

        return new Lookup() {
            @Override
            public <T> Item<T> lookupItem(Template<T> template) {
                return lookup.lookupItem(template);
            }

            @Override
            public <T> Result<T> lookupResult(Class<T> clazz) {
                return lookup.lookupResult(clazz);
            }

            @Override
            public <T> Collection<? extends T> lookupAll(Class<T> clazz) {
                return lookup.lookupAll(clazz);
            }

            @Override
            public <T> T lookup(Class<T> arg0) {
                return lookup.lookup(arg0);
            }

            @Override
            public <T> Result<T> lookup(Template<T> arg0) {
                return lookup.lookup(arg0);
            }
        };
    }
}
