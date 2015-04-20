package org.netbeans.gradle.model.java;

import java.io.File;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.tasks.TaskCollection;
import org.netbeans.gradle.model.api.ProjectInfoBuilder;
import org.netbeans.gradle.model.gradleclasses.GradleClass;
import org.netbeans.gradle.model.gradleclasses.GradleClasses;
import org.netbeans.gradle.model.util.BuilderUtils;

public enum JarOutputsModelBuilder
implements
        ProjectInfoBuilder<JarOutputsModel> {

    INSTANCE;

    private static final Logger LOGGER = Logger.getLogger(JarOutputsModelBuilder.class.getName());

    // Jar tasks may contain an extension property with this name
    // to explicitly declare to which source set they belong to.
    private static final String SOURCE_SET_NAME_PROPERTY = "netBeansSourceSets";

    public JarOutputsModel getProjectInfo(Project project) {
        if (!project.getPlugins().hasPlugin("java")) {
            return null;
        }

        Class<? extends Task> jarClass = GradleClasses.tryGetGradleClass(
                project, "org.gradle.api.tasks.bundling.Jar", Task.class);

        if (jarClass == null) {
            LOGGER.warning("Cannot find class of Jar tasks.");
            return new JarOutputsModel(Collections.<JarOutput>emptySet());
        }

        List<JarOutput> result = new LinkedList<JarOutput>();
        TaskCollection<? extends Task> allJars = project.getTasks().withType(jarClass);

        for (Task jar: allJars) {
            result.add(new JarOutput(
                    jar.getName(),
                    (File)jar.property("archivePath"),
                    tryGetAllJarSourceSetNames(project, jar)));
        }

        return new JarOutputsModel(result);
    }

    private static String tryGetName(Project project, Object sourceSetObj) {
        GradleClass sourceSetType;
        try {
            sourceSetType = GradleClasses.getGradleClass(project, "org.gradle.api.tasks.SourceSet");
        } catch (ClassNotFoundException ex) {
            LOGGER.log(Level.WARNING, "The class SourceSet was not found", ex);
            return null;
        }

        if (!sourceSetType.getType().isInstance(sourceSetObj)) {
            return null;
        }

        Method getName;
        try {
            getName = sourceSetType.getMethod("getName");
        } catch (NoSuchMethodException ex) {
            LOGGER.log(Level.WARNING, "Missing SourceSet.getName method.", ex);
            return null;
        }

        try {
            Object result = getName.invoke(sourceSetObj);
            return result != null ? result.toString() : null;
        } catch (IllegalAccessException ex) {
            LOGGER.log(Level.WARNING, "Failed to call SourceSet.getName", ex);
            return null;
        } catch (IllegalArgumentException ex) {
            LOGGER.log(Level.WARNING, "Failed to call SourceSet.getName", ex);
            return null;
        } catch (InvocationTargetException ex) {
            LOGGER.log(Level.WARNING, "Failed to call SourceSet.getName", ex);
            return null;
        }
    }

    private static String tryGetJarSourceSetName(Project project, Object sourceSetObj) {
        String result = tryGetName(project, sourceSetObj);
        if (result != null) {
            return result;
        }

        return sourceSetObj.toString();
    }

    private static Set<String> tryGetAllJarSourceSetNames(Project project, Task jar) {
        ExtraPropertiesExtension extProperties = jar.getExtensions().getExtraProperties();
        Object sourceSetObjs = extProperties.has(SOURCE_SET_NAME_PROPERTY)
                ? extProperties.get(SOURCE_SET_NAME_PROPERTY)
                : null;
        if (sourceSetObjs == null) {
            return null;
        }

        if (sourceSetObjs instanceof Iterable) {
            Set<String> sourceSetNames = new HashSet<String>();
            for (Object sourceSetObj: (Iterable<?>)sourceSetObjs) {
                String name = tryGetJarSourceSetName(project, sourceSetObj);
                if (name != null) {
                    sourceSetNames.add(name);
                }
            }
            return sourceSetNames;
        }
        else {
            String name = tryGetJarSourceSetName(project, sourceSetObjs);
            return name != null ? Collections.singleton(name) : Collections.<String>emptySet();
        }
    }

    /** {@inheritDoc } */
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }
}
