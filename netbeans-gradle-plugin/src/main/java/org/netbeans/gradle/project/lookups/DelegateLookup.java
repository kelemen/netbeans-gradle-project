package org.netbeans.gradle.project.lookups;

import java.util.Collection;
import org.jtrim.utils.ExceptionHelper;
import org.openide.util.Lookup;

public class DelegateLookup extends Lookup {
    protected final Lookup wrapped;

    public DelegateLookup(Lookup wrapped) {
        ExceptionHelper.checkNotNullArgument(wrapped, "wrapped");
        this.wrapped = wrapped;
    }

    @Override
    public <T> T lookup(Class<T> clazz) {
        return wrapped.lookup(clazz);
    }

    @Override
    public <T> Result<T> lookup(Template<T> template) {
        return wrapped.lookup(template);
    }

    @Override
    public <T> Collection<? extends T> lookupAll(Class<T> clazz) {
        return wrapped.lookupAll(clazz);
    }

    @Override
    public <T> Result<T> lookupResult(Class<T> clazz) {
        return wrapped.lookupResult(clazz);
    }

    @Override
    public <T> Item<T> lookupItem(Template<T> template) {
        return wrapped.lookupItem(template);
    }
}
