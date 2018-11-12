package org.netbeans.gradle.project.tasks;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbGradleProjectFactory;
import org.netbeans.gradle.project.api.config.GradleArgumentQuery;
import org.netbeans.gradle.project.api.task.DaemonTaskContext;
import org.netbeans.gradle.project.extensions.NbGradleExtensionRef;
import org.netbeans.gradle.project.model.SettingsGradleDef;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.standard.UserInitScriptPath;

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
            DaemonTaskContext context,
            List<String> result,
            ArgGetter argGetter) {

        NbGradleProject gradleProject = NbGradleProjectFactory.tryGetGradleProject(context.getProject());
        if (gradleProject == null) {
            return;
        }

        for (NbGradleExtensionRef extensionRef: gradleProject.getExtensions().getExtensionRefs()) {
            for (GradleArgumentQuery query: extensionRef.getExtensionLookup().lookupAll(GradleArgumentQuery.class)) {
                List<String> extArgs = fixArguments(extensionRef.getName(), argGetter.getArgs(query, context));
                result.addAll(extArgs);
            }
        }
    }

    private static void addExtensionArgs(DaemonTaskContext context, List<String> result) {
        addExtensionArgs(context, result, GradleArgumentQuery::getExtraArgs);
    }

    private static void addExtensionJvmArgs(DaemonTaskContext context, List<String> result) {
        addExtensionArgs(context, result, GradleArgumentQuery::getExtraJvmArgs);
    }

    private static Path tryGetUserInitScript(Project project) {
        NbGradleProject gradleProject = NbGradleProjectFactory.tryGetGradleProject(project);
        if (gradleProject == null) {
            return null;
        }

        UserInitScriptPath path = gradleProject.getCommonProperties().userInitScriptPath().getActiveValue();
        return path != null ? path.getPath(gradleProject) : null;
    }

    public static List<String> getExtraArgs(
            SettingsGradleDef preferredSettings,
            DaemonTaskContext context) {
        List<String> result = new ArrayList<>();

        result.addAll(emptyIfNull(CommonGlobalSettings.getDefault().gradleArgs().getActiveValue()));

        addExtensionArgs(context, result);

        Path settingsGradle = preferredSettings.getSettingsGradle();
        if (settingsGradle != null) {
            result.add("-c");
            result.add(settingsGradle.toString());
        }

        Path userInitScript = tryGetUserInitScript(context.getProject());
        if (userInitScript != null) {
            result.add("--init-script");
            result.add(userInitScript.toString());
        }

        if (context.isModelLoading()) {
            result.add("-PevaluatingIDE=NetBeans");
        }

        return result;
    }

    public static List<String> getExtraJvmArgs(DaemonTaskContext context) {
        List<String> result = new ArrayList<>();

        result.addAll(emptyIfNull(CommonGlobalSettings.getDefault().gradleJvmArgs().getActiveValue()));
        addExtensionJvmArgs(context, result);

        return result;
    }

    private static interface ArgGetter {
        public List<String> getArgs(GradleArgumentQuery query, DaemonTaskContext context);
    }

    private GradleArguments() {
        throw new AssertionError();
    }
}
