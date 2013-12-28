package org.netbeans.gradle.project;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.project.SubprojectProvider;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.Parameters;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class ProjectLookupHack extends ProxyLookup {
    private static final Logger LOGGER = Logger.getLogger(ProjectLookupHack.class.getName());

    public interface LookupContainer {
        public NbGradleProject getProject();
        public Lookup getLookup();
        public Lookup getLookupAndActivate();
    }

    private final AtomicBoolean activated;
    private final LookupContainer lookupContainer;

    public ProjectLookupHack(LookupContainer lookupContainer) {
        Parameters.notNull("lookupContainer", lookupContainer);
        this.lookupContainer = lookupContainer;
        this.activated = new AtomicBoolean(false);

        setLookups(new AccessPreventerLookup());
    }

    private Lookup activate() {
        Lookup result = lookupContainer.getLookupAndActivate();
        if (activated.compareAndSet(false, true)) {
            setLookups(result);
        }
        return result;
    }

    private class AccessPreventerLookup extends Lookup {
        private final Map<Class<?>, Lookup> typeActions;

        public AccessPreventerLookup() {
            this.typeActions = new HashMap<Class<?>, Lookup>();
            typeActions.put(SubprojectProvider.class, Lookup.EMPTY);
            typeActions.put(ClassPathProvider.class, Lookups.singleton(new UnimportantRootClassPathProvider()));

            Lookup wrappedLookup = lookupContainer.getLookup();
            typeActions.put(ProjectInformation.class, wrappedLookup);
        }

        private Lookup lookupForType(Class<?> type) {
            Lookup action = typeActions.get(type);
            if (action != null) {
                LOGGER.log(Level.INFO, "Using custom lookup for type {0}", type.getName());
                return action;
            }
            else {
                LOGGER.log(Level.INFO, "Activating project lookup because of the request type: {0}", type.getName());
                return activate();
            }
        }

        @Override
        public <T> T lookup(Class<T> clazz) {
            return lookupForType(clazz).lookup(clazz);
        }

        @Override
        public <T> Result<T> lookup(Template<T> template) {
            return lookupForType(template.getType()).lookup(template);
        }

        @Override
        public <T> Collection<? extends T> lookupAll(Class<T> clazz) {
            return lookupForType(clazz).lookupAll(clazz);
        }

        @Override
        public <T> Result<T> lookupResult(Class<T> clazz) {
            return lookupForType(clazz).lookupResult(clazz);
        }

        @Override
        public <T> Item<T> lookupItem(Template<T> template) {
            return lookupForType(template.getType()).lookupItem(template);
        }
    }

    private class UnimportantRootClassPathProvider implements ClassPathProvider {
        @Override
        public ClassPath findClassPath(FileObject file, String type) {
            Lookup lookup;
            if (lookupContainer.getProject().getProjectDirectory().equals(file)) {
                lookup = lookupContainer.getLookup();
            }
            else {
                lookup = activate();
            }

            for (ClassPathProvider otherProvider: lookup.lookupAll(ClassPathProvider.class)) {
                ClassPath result = otherProvider.findClassPath(file, type);
                if (result != null) {
                    return result;
                }
            }
            return null;
        }
    }
}
