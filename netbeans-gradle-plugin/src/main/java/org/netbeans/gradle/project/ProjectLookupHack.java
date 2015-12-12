package org.netbeans.gradle.project;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.classpath.ClassPath;
import org.netbeans.api.project.ProjectInformation;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.java.query.GradleClassPathProvider;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.NbGradleSingleProjectConfigProvider;
import org.netbeans.gradle.project.api.config.ProjectSettingsProvider;
import org.netbeans.spi.java.classpath.ClassPathProvider;
import org.netbeans.spi.project.ActionProvider;
import org.netbeans.spi.project.AuxiliaryConfiguration;
import org.netbeans.spi.project.AuxiliaryProperties;
import org.netbeans.spi.project.ProjectConfigurationProvider;
import org.netbeans.spi.project.SubprojectProvider;
import org.netbeans.spi.project.ui.CustomizerProvider;
import org.netbeans.spi.queries.FileEncodingQueryImplementation;
import org.netbeans.spi.queries.SharabilityQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;
import org.openide.util.lookup.ProxyLookup;

public final class ProjectLookupHack extends ProxyLookup {
    private static final Logger LOGGER = Logger.getLogger(ProjectLookupHack.class.getName());

    private static final AtomicReference<Collection<Class<?>>> NOT_IMPLEMENTED_SERVICES
            = new AtomicReference<>();

    public interface LookupContainer {
        public NbGradleProject getProject();
        public Lookup getLookup();
        public Lookup getLookupAndActivate();
    }

    private final AtomicBoolean activated;
    private final LookupContainer lookupContainer;

    public ProjectLookupHack(LookupContainer lookupContainer) {
        ExceptionHelper.checkNotNullArgument(lookupContainer, "lookupContainer");

        this.lookupContainer = lookupContainer;
        this.activated = new AtomicBoolean(false);

        setLookups(new AccessPreventerLookup());
    }

    private Lookup activate(Object reason) {
        Lookup result = lookupContainer.getLookupAndActivate();
        if (activated.compareAndSet(false, true)) {
            FileObject projectDir = lookupContainer.getProject().getProjectDirectory();
            LOGGER.log(Level.INFO,
                    "Activating project ({0}) lookup due to requesting: {1}",
                    new Object[]{projectDir, reason});
            setLookups(result);
        }
        return result;
    }

    private static void tryAddClass(String className, Collection<Class<?>> result) {
        try {
            result.add(Class.forName(className));
        } catch (ClassNotFoundException ex) {
        }
    }

    private static Collection<Class<?>> getNotImplementedServices() {
        Collection<Class<?>> result = NOT_IMPLEMENTED_SERVICES.get();
        if (result == null) {
            result = new LinkedList<>();

            // We could implement these interfaces but do not want to
            // because for this information we need to parse the build script
            // which is too slow to be useful in the project open dialog.
            result.add(SubprojectProvider.class);
            tryAddClass("org.netbeans.spi.project.ProjectContainerProvider", result);
            tryAddClass("org.netbeans.spi.project.DependencyProjectProvider", result);
            NOT_IMPLEMENTED_SERVICES.set(Collections.unmodifiableCollection(result));
            result = NOT_IMPLEMENTED_SERVICES.get();
        }
        return result;
    }

    private class AccessPreventerLookup extends Lookup {
        private final Map<String, Lookup> typeActions;

        public AccessPreventerLookup() {
            this.typeActions = new HashMap<>();
            for (Class<?> type: getNotImplementedServices()) {
                typeActions.put(type.getName(), Lookup.EMPTY);
            }
            typeActions.put(ClassPathProvider.class.getName(), Lookups.singleton(new UnimportantRootClassPathProvider()));

            Lookup wrappedLookup = lookupContainer.getLookup();
            typeActions.put(AuxiliaryProperties.class.getName(), wrappedLookup);
            typeActions.put(AuxiliaryConfiguration.class.getName(), wrappedLookup);
            typeActions.put(GradleClassPathProvider.class.getName(), wrappedLookup);
            typeActions.put(NbGradleProject.class.getName(), wrappedLookup);
            typeActions.put(FileEncodingQueryImplementation.class.getName(), wrappedLookup);
            typeActions.put(ProjectInformation.class.getName(), wrappedLookup);
            typeActions.put(ActionProvider.class.getName(), wrappedLookup);
            typeActions.put(CustomizerProvider.class.getName(), wrappedLookup);
            typeActions.put(NbGradleSingleProjectConfigProvider.class.getName(), wrappedLookup);
            typeActions.put(NbGradleCommonProperties.class.getName(), wrappedLookup);
            typeActions.put(ProjectConfigurationProvider.class.getName(), wrappedLookup);
            typeActions.put(JavaExtension.class.getName(), wrappedLookup);
            typeActions.put(SharabilityQueryImplementation2.class.getName(), wrappedLookup);
            typeActions.put(ProjectSettingsProvider.class.getName(), wrappedLookup);
            typeActions.put("org.netbeans.modules.maven.NbMavenProjectImpl", wrappedLookup);
            typeActions.put("org.netbeans.modules.web.browser.spi.ProjectBrowserProvider", wrappedLookup);
            typeActions.put("org.netbeans.spi.project.ui.ProjectProblemsProvider", wrappedLookup);
        }

        private Lookup lookupForType(Class<?> type) {
            Lookup action = typeActions.get(type.getName());
            if (action != null) {
                LOGGER.log(Level.INFO, "Using custom lookup for type {0}", type.getName());
                return action;
            }
            else {
                return activate(type.getName());
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
                lookup = activate("ClassPathProvider.findClassPath");
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
