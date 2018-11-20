package org.netbeans.gradle.model.java;

import java.util.Collections;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.plugins.ExtraPropertiesExtension;
import org.gradle.api.plugins.JavaPluginConvention;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.TaskCollection;
import org.gradle.api.tasks.bundling.Jar;
import org.netbeans.gradle.model.api.ProjectInfoBuilder2;
import org.netbeans.gradle.model.util.BuilderUtils;

enum JarOutputsModelBuilder
implements
        ProjectInfoBuilder2<JarOutputsModel> {

    INSTANCE;

    // Jar tasks may contain an extension property with this name
    // to explicitly declare to which source set they belong to.
    private static final String SOURCE_SET_NAME_PROPERTY = "netBeansSourceSets";

    @Override
    public JarOutputsModel getProjectInfo(Object project) {
        return getProjectInfo((Project)project);
    }

    private JarOutputsModel getProjectInfo(Project project) {
        if (project.getConvention().findPlugin(JavaPluginConvention.class) == null) {
            return null;
        }

        List<JarOutput> result = new LinkedList<JarOutput>();
        TaskCollection<? extends Jar> allJars = project.getTasks().withType(Jar.class);

        for (Jar jar: allJars) {
            result.add(new JarOutput(
                    jar.getName(),
                    jar.getArchivePath(),
                    tryGetAllJarSourceSetNames(jar)));
        }

        return new JarOutputsModel(result);
    }

    private static String tryGetName(Object sourceSetObj) {
        return sourceSetObj instanceof SourceSet
                ? ((SourceSet)sourceSetObj).getName()
                : null;
    }

    private static String tryGetJarSourceSetName(Object sourceSetObj) {
        String result = tryGetName(sourceSetObj);
        if (result != null) {
            return result;
        }

        return sourceSetObj.toString();
    }

    private static Set<String> tryGetAllJarSourceSetNames(Task jar) {
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
                String name = tryGetJarSourceSetName(sourceSetObj);
                if (name != null) {
                    sourceSetNames.add(name);
                }
            }
            return sourceSetNames;
        }
        else {
            String name = tryGetJarSourceSetName(sourceSetObjs);
            return name != null ? Collections.singleton(name) : Collections.<String>emptySet();
        }
    }

    /** {@inheritDoc } */
    @Override
    public String getName() {
        return BuilderUtils.getNameForEnumBuilder(this);
    }
}
