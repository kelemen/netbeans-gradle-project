package org.netbeans.gradle.project;

import java.util.List;
import org.openide.util.Lookup;
import org.openide.util.lookup.ProxyLookup;

final class DynamicLookup extends ProxyLookup {
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
}
