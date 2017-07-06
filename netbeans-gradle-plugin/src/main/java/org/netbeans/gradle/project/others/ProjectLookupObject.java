package org.netbeans.gradle.project.others;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.openide.util.Lookup;

public final class ProjectLookupObject {
    private static final Logger LOGGER = Logger.getLogger(ProjectLookupObject.class.getName());

    private final PluginClass pluginClass;

    public ProjectLookupObject(PluginClass pluginClass) {
        this.pluginClass = Objects.requireNonNull(pluginClass, "pluginClass");
    }

    private static Map<Class<?>, Object> createArgs(Project project) {
        Map<Class<?>, Object> result = new HashMap<>();
        result.put(Project.class, project);
        result.put(Lookup.class, Lookup.EMPTY);
        return result;
    }

    public Object tryCreateInstance(Project project) {
        Class<?> type = pluginClass.tryGetClass();
        if (type == null) {
            return null;
        }

        try {
            return ReflectionHelper.tryCreateInstance(type, createArgs(project));
        } catch (Throwable ex) {
            LOGGER.log(Level.INFO, "Creating an instance of " + type + " has thrown an exception.", ex);
            return null;
        }
    }
}
