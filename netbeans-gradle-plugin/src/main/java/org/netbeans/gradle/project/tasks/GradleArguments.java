package org.netbeans.gradle.project.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleExtensionRef;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.GradleArgumentQuery;
import org.netbeans.gradle.project.api.task.DaemonTaskContext;
import org.netbeans.gradle.project.properties.global.GlobalGradleSettings;

public final class GradleArguments {
    private static final Logger LOGGER = Logger.getLogger(GradleArguments.class.getName());

    private static <V> List<V> emptyIfNull(List<V> list) {
        return list != null ? list : Collections.<V>emptyList();
    }

    private static List<String> filterNulls(List<String> args) {
        List<String> result = new ArrayList<>(args.size());
        for (String arg: args) {
            if (arg != null) {
                result.add(arg);
            }
        }
        return result;
    }

    private static List<String> fixArguments(String extensionName, List<String> args) {
        if (args == null) {
            LOGGER.log(Level.WARNING, "Extension {0} returned null argument list.", extensionName);
            return Collections.emptyList();
        }

        for (String arg: args) {
            if (arg == null) {
                LOGGER.log(Level.WARNING, "Extension {0} returned a null argument.", extensionName);
                return filterNulls(args);
            }
        }

        return args;
    }

    private static void addExtensionArgs(
            Project project,
            DaemonTaskContext context,
            List<String> result,
            ArgGetter argGetter) {

        NbGradleProject gradleProject = project.getLookup().lookup(NbGradleProject.class);
        if (gradleProject == null) {
            return;
        }

        for (NbGradleExtensionRef extensionRef: gradleProject.getExtensionRefs()) {
            for (GradleArgumentQuery query: extensionRef.getExtensionLookup().lookupAll(GradleArgumentQuery.class)) {
                List<String> extArgs = fixArguments(extensionRef.getName(), argGetter.getArgs(query, context));
                result.addAll(extArgs);
            }
        }
    }

    private static void addExtensionArgs(Project project, DaemonTaskContext context, List<String> result) {
        addExtensionArgs(project, context, result, new ArgGetter() {
            @Override
            public List<String> getArgs(GradleArgumentQuery query, DaemonTaskContext context) {
                return query.getExtraArgs(context);
            }
        });
    }

    private static void addExtensionJvmArgs(Project project, DaemonTaskContext context, List<String> result) {
        addExtensionArgs(project, context, result, new ArgGetter() {
            @Override
            public List<String> getArgs(GradleArgumentQuery query, DaemonTaskContext context) {
                return query.getExtraJvmArgs(context);
            }
        });
    }

    public static List<String> getExtraArgs(
            Project project,
            Path preferredSettings,
            DaemonTaskContext context) {

        List<String> result = new LinkedList<>();

        result.addAll(emptyIfNull(GlobalGradleSettings.getDefault().gradleArgs().getValue()));

        addExtensionArgs(project, context, result);

        if (preferredSettings != null) {
            result.add("-c");
            result.add(preferredSettings.toString());
        }

        if (context.isModelLoading()) {
            result.add("-PevaluatingIDE=NetBeans");
        }

        return result;
    }

    public static List<String> getExtraJvmArgs(
            Project project,
            DaemonTaskContext context) {

        List<String> result = new LinkedList<>();

        result.addAll(emptyIfNull(GlobalGradleSettings.getDefault().gradleJvmArgs().getValue()));
        addExtensionJvmArgs(project, context, result);

        return result;
    }

    private static interface ArgGetter {
        public List<String> getArgs(GradleArgumentQuery query, DaemonTaskContext context);
    }

    private GradleArguments() {
        throw new AssertionError();
    }
}
