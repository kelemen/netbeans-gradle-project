package org.netbeans.gradle.project.extensions;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.gradle.model.util.CollectionUtils;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension2;
import org.netbeans.gradle.project.api.entry.GradleProjectExtensionDef;
import org.netbeans.gradle.project.properties.ExtensionProjectSettingsPageDefs;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

public final class ExtensionLoader {
    private static final Logger LOGGER = Logger.getLogger(ExtensionLoader.class.getName());

    private static Level levelFromException(Throwable exception) {
        if (exception instanceof RuntimeException) {
            return Level.WARNING;
        }
        else if (exception instanceof Error) {
            return Level.SEVERE;
        }
        else {
            return Level.INFO;
        }
    }

    private static <ModelType> NbGradleExtensionRef loadExtension(
            NbGradleProject project,
            GradleProjectExtensionDef<ModelType> def) throws IOException {

        try {
            GradleProjectExtension2<ModelType> extension = def.createExtension(project);
            return createExtensionRef(project, def, extension);
        } catch (Throwable ex) {
            LOGGER.log(levelFromException(ex),
                    "Failed to load extension: " + def.getName() + " for project " + project.getProjectDirectory(),
                    ex);
            return null;
        }
    }

    private static <ModelType> NbGradleExtensionRef createExtensionRef(
            final NbGradleProject project,
            GradleProjectExtensionDef<ModelType> def,
            GradleProjectExtension2<ModelType> extension) {

        return new NbGradleExtensionRef(def, extension, (NbGradleExtensionRef extensionRef, Lookup... lookups) -> {
            String extensionName = extensionRef.getName();
            return Lookups.fixed(new ExtensionProjectSettingsPageDefs(project, extensionName, lookups));
        });
    }

    private static void tryAddExtension(
            Object sourceDef,
            NbGradleExtensionRef extension,
            List<NbGradleExtensionRef> result,
            Set<String> alreadyLoaded) {
        if (extension == null) {
            return;
        }

        if (alreadyLoaded.contains(extension.getName())) {
            LOGGER.log(Level.WARNING, "Extension has already been loaded with the same name: {0}. Ignoring implementation: {1}",
                    new Object[]{extension.getName(), sourceDef.getClass().getName()});
        }
        else {
            result.add(extension);
        }
    }

    @SuppressWarnings("unchecked")
    private static Class<GradleProjectExtensionDef<?>> defClass() {
        return (Class<GradleProjectExtensionDef<?>>)(Class<?>)GradleProjectExtensionDef.class;
    }

    public static List<NbGradleExtensionRef> loadExtensions(NbGradleProject project) throws IOException {
        Lookup defaultLookup = Lookup.getDefault();

        Collection<? extends GradleProjectExtensionDef<?>> defs
                = defaultLookup.lookupAll(defClass());

        int expectedExtensionCount = defs.size();
        List<NbGradleExtensionRef> result = new ArrayList<>(expectedExtensionCount);
        Set<String> alreadyLoaded = CollectionUtils.newHashSet(expectedExtensionCount);

        for (GradleProjectExtensionDef<?> def: defs) {
            NbGradleExtensionRef loadedExtension = loadExtension(project, def);
            tryAddExtension(def, loadedExtension, result, alreadyLoaded);
        }

        return result;
    }

    private ExtensionLoader() {
        throw new AssertionError();
    }
}
