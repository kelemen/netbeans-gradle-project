package org.netbeans.gradle.project.properties.global;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.jtrim.property.PropertyFactory;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.properties.GenericProfileSettings;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.GradleLocationDef;
import org.netbeans.gradle.project.properties.GradleLocationDirectory;
import org.netbeans.gradle.project.properties.ModelLoadingStrategy;
import org.netbeans.gradle.project.properties.MultiProfileProperties;
import org.netbeans.gradle.project.properties.NbGradleCommonProperties;
import org.netbeans.gradle.project.properties.ProfileSettingsContainer;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.SingleProfileSettingsEx;
import org.netbeans.gradle.project.properties.standard.CommonProperties;
import org.netbeans.gradle.project.tasks.vars.StringResolvers;
import org.netbeans.gradle.project.util.NbConsumer;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

import static org.netbeans.gradle.project.properties.standard.CommonProperties.*;

public final class CommonGlobalSettings {
    private final ActiveSettingsQuery activeSettingsQuery;

    private final PropertyReference<ScriptPlatform> defaultJdk;

    private final PropertyReference<GradleLocationDef> gradleLocation;
    private final PropertyReference<File> gradleUserHomeDir;
    private final PropertyReference<PlatformOrder> platformPreferenceOrder;

    private final PropertyReference<List<String>> gradleArgs;
    private final PropertyReference<List<String>> gradleJvmArgs;

    private final PropertyReference<Boolean> skipTests;
    private final PropertyReference<Boolean> skipCheck;
    private final PropertyReference<Boolean> alwaysClearOutput;
    private final PropertyReference<Boolean> mayRelyOnJavaOfScript;
    private final PropertyReference<Boolean> compileOnSave;
    private final PropertyReference<Boolean> replaceLfOnStdIn;
    private final PropertyReference<Boolean> askBeforeCancelExec;
    private final PropertyReference<Boolean> loadRootProjectFirst;
    private final PropertyReference<Boolean> showGradleVersion;

    private final PropertyReference<Boolean> detectProjectDependenciesByJarName;
    private final PropertyReference<SelfMaintainedTasks> selfMaintainedTasks;
    private final PropertyReference<ModelLoadingStrategy> modelLoadingStrategy;

    private final PropertyReference<Integer> projectCacheSize;
    private final PropertyReference<Integer> gradleDaemonTimeoutSec;

    public CommonGlobalSettings(ActiveSettingsQuery activeSettingsQuery) {
        ExceptionHelper.checkNotNullArgument(activeSettingsQuery, "activeSettingsQuery");
        this.activeSettingsQuery = activeSettingsQuery;

        this.platformPreferenceOrder = platformPreferenceOrder(activeSettingsQuery);
        this.defaultJdk = defaultJdk(activeSettingsQuery, this.platformPreferenceOrder);
        this.gradleLocation = gradleLocation(activeSettingsQuery);
        this.gradleUserHomeDir = gradleUserHomeDir(activeSettingsQuery);
        this.gradleArgs = gradleArgs(activeSettingsQuery);
        this.gradleJvmArgs = gradleJvmArgs(activeSettingsQuery);
        this.skipTests = skipTests(activeSettingsQuery);
        this.skipCheck = skipCheck(activeSettingsQuery);
        this.alwaysClearOutput = alwaysClearOutput(activeSettingsQuery);
        this.mayRelyOnJavaOfScript = mayRelyOnJavaOfScript(activeSettingsQuery);
        this.compileOnSave = compileOnSave(activeSettingsQuery);
        this.replaceLfOnStdIn = replaceLfOnStdIn(activeSettingsQuery);
        this.askBeforeCancelExec = askBeforeCancelExec(activeSettingsQuery);
        this.loadRootProjectFirst = loadRootProjectFirst(activeSettingsQuery);
        this.detectProjectDependenciesByJarName = detectProjectDependenciesByJarName(activeSettingsQuery);
        this.selfMaintainedTasks = selfMaintainedTasks(activeSettingsQuery);
        this.modelLoadingStrategy = modelLoadingStrategy(activeSettingsQuery);
        this.projectCacheSize = projectCacheSize(activeSettingsQuery);
        this.gradleDaemonTimeoutSec = gradleDaemonTimeoutSec(activeSettingsQuery);
        this.showGradleVersion = showGradleVersion(activeSettingsQuery);
    }

    public static PropertyReference<ScriptPlatform> defaultJdk(ActiveSettingsQuery activeSettingsQuery) {
        return defaultJdk(activeSettingsQuery, platformPreferenceOrder(activeSettingsQuery));
    }

    private static PropertyReference<ScriptPlatform> defaultJdk(
            ActiveSettingsQuery activeSettingsQuery,
            PropertyReference<PlatformOrder> orderRef) {
        return NbGradleCommonProperties.scriptPlatform(activeSettingsQuery, orderRef.getActiveSource());
    }

    public PropertyReference<ScriptPlatform> defaultJdk() {
        return defaultJdk;
    }

    public static PropertyReference<GradleLocationDef> gradleLocation(ActiveSettingsQuery activeSettingsQuery) {
        return NbGradleCommonProperties.gradleLocation(activeSettingsQuery);
    }

    public PropertyReference<GradleLocationDef> gradleLocation() {
        return gradleLocation;
    }

    public static PropertyReference<File> gradleUserHomeDir(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineFileProperty("gradle", "user-home-dir"), activeSettingsQuery, null);
    }

    public PropertyReference<File> gradleUserHomeDir() {
        return gradleUserHomeDir;
    }

    public static PropertyReference<PlatformOrder> platformPreferenceOrder(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(
                definePlatformOrderProperty("platforms", "preference-order"),
                activeSettingsQuery,
                PlatformOrder.DEFAULT_ORDER);
    }

    public PropertyReference<PlatformOrder> platformPreferenceOrder() {
        return platformPreferenceOrder;
    }

    public static PropertyReference<List<String>> gradleArgs(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(
                defineStringListProperty("gradle", "args"),
                activeSettingsQuery,
                Collections.<String>emptyList());
    }

    public PropertyReference<List<String>> gradleArgs() {
        return gradleArgs;
    }

    public static PropertyReference<List<String>> gradleJvmArgs(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(
                defineStringListProperty("gradle", "jvm-args"),
                activeSettingsQuery,
                Collections.<String>emptyList());
    }

    public PropertyReference<List<String>> gradleJvmArgs() {
        return gradleJvmArgs;
    }

    public static PropertyReference<Boolean> skipTests(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineBooleanProperty("tasks", "skip-tests"), activeSettingsQuery, false);
    }

    public PropertyReference<Boolean> skipTests() {
        return skipTests;
    }

    public static PropertyReference<Boolean> skipCheck(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineBooleanProperty("tasks", "skip-check"), activeSettingsQuery, false);
    }

    public PropertyReference<Boolean> skipCheck() {
        return skipCheck;
    }

    public static PropertyReference<Boolean> alwaysClearOutput(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineBooleanProperty("tasks", "always-clear-output"), activeSettingsQuery, false);
    }

    public PropertyReference<Boolean> alwaysClearOutput() {
        return alwaysClearOutput;
    }

    public static PropertyReference<Boolean> mayRelyOnJavaOfScript(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineBooleanProperty("tasks", "rely-on-java-of-script"), activeSettingsQuery, false);
    }

    public PropertyReference<Boolean> mayRelyOnJavaOfScript() {
        return mayRelyOnJavaOfScript;
    }

    public static PropertyReference<Boolean> compileOnSave(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineBooleanProperty("compile", "compile-on-save"), activeSettingsQuery, false);
    }

    public PropertyReference<Boolean> compileOnSave() {
        return compileOnSave;
    }

    public static PropertyReference<Boolean> replaceLfOnStdIn(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineBooleanProperty("hacks", "replace-lf-on-stdin"), activeSettingsQuery, true);
    }

    public PropertyReference<Boolean> replaceLfOnStdIn() {
        return replaceLfOnStdIn;
    }

    public static PropertyReference<Boolean> askBeforeCancelExec(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineBooleanProperty("tasks", "ask-before-cancel-exec"), activeSettingsQuery, true);
    }

    public PropertyReference<Boolean> askBeforeCancelExec() {
        return askBeforeCancelExec;
    }

    public static PropertyReference<Boolean> loadRootProjectFirst(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineBooleanProperty("hacks", "load-root-first"), activeSettingsQuery, true);
    }

    public PropertyReference<Boolean> loadRootProjectFirst() {
        return loadRootProjectFirst;
    }

    public static PropertyReference<Boolean> detectProjectDependenciesByJarName(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(
                defineBooleanProperty("hacks", "detect-project-dep-by-jar-name"),
                activeSettingsQuery,
                false);
    }

    public PropertyReference<Boolean> detectProjectDependenciesByJarName() {
        return detectProjectDependenciesByJarName;
    }

    public static PropertyReference<SelfMaintainedTasks> selfMaintainedTasks(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(
                defineEnumProperty(SelfMaintainedTasks.class, "hacks", "self-maintained-tasks"),
                activeSettingsQuery,
                SelfMaintainedTasks.FALSE);
    }

    public PropertyReference<SelfMaintainedTasks> selfMaintainedTasks() {
        return selfMaintainedTasks;
    }

    public static PropertyReference<ModelLoadingStrategy> modelLoadingStrategy(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(
                defineEnumProperty(ModelLoadingStrategy.class, "model-loading", "strategy"),
                activeSettingsQuery,
                ModelLoadingStrategy.NEWEST_POSSIBLE);
    }

    public PropertyReference<ModelLoadingStrategy> modelLoadingStrategy() {
        return modelLoadingStrategy;
    }

    public static PropertyReference<Boolean> showGradleVersion(ActiveSettingsQuery activeSettingsQuery) {
        return NbGradleCommonProperties.showGradleVersion(activeSettingsQuery);
    }

    public PropertyReference<Boolean> showGradleVersion() {
        return showGradleVersion;
    }

    public static PropertyReference<Integer> projectCacheSize(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineIntProperty("cache", "size"), activeSettingsQuery, 100);
    }

    public PropertyReference<Integer> projectCacheSize() {
        return projectCacheSize;
    }

    public static PropertyReference<Integer> gradleDaemonTimeoutSec(ActiveSettingsQuery activeSettingsQuery) {
        return propertyRef(defineIntProperty("daemon", "timeout-seconds"), activeSettingsQuery, null);
    }

    public PropertyReference<Integer> gradleDaemonTimeoutSec() {
        return gradleDaemonTimeoutSec;
    }

    public File tryGetGradleInstallationAsFile() {
        GradleLocationDef locationDef = gradleLocation.getActiveValue();
        GradleLocation location = locationDef.getLocation(StringResolvers.getDefaultGlobalResolver());
        if (location instanceof GradleLocationDirectory) {
            return ((GradleLocationDirectory)location).tryGetGradleHome();
        }
        return null;
    }

    public FileObject tryGetGradleInstallation() {
        File result = tryGetGradleInstallationAsFile();
        return result != null ? FileUtil.toFileObject(result) : null;
    }

    private static ActiveSettingsQuery loadDefaultActiveSettings() {
        ProfileSettingsContainer settingsContainer = ProfileSettingsContainer.getDefault();
        List<SingleProfileSettingsEx> settings = settingsContainer
                .loadAllProfileSettings(GlobalProfileSettingsKey.GLOBAL_DEFAULTS_KEY.getWithFallbacks());
        return new MultiProfileProperties(settings);
    }

    // For testing purposes
    public static void withCleanMemorySettings(NbConsumer<? super GenericProfileSettings> task) {
        DefaultHolder.withCleanMemorySettings(task);
    }

    public static CommonGlobalSettings getDefault() {
        return DefaultHolder.DEFAULT;
    }

    public static ActiveSettingsQuery getDefaultActiveSettingsQuery() {
        return DefaultHolder.DEFAULT_ACTIVE_SETTINGS;
    }

    public ActiveSettingsQuery getActiveSettingsQuery() {
        return activeSettingsQuery;
    }

    private static <T> PropertyReference<T> propertyRef(
            PropertyDef<?, T> propertyDef,
            ActiveSettingsQuery activeSettingsQuery,
            T defaultValue) {
        return new PropertyReference<>(propertyDef, activeSettingsQuery, PropertyFactory.constSource(defaultValue));
    }

    private static PropertyDef<?, PlatformOrder> definePlatformOrderProperty(String... keyPath) {
        PropertyDef.Builder<PlatformOrder, PlatformOrder> result = new PropertyDef.Builder<>(ConfigPath.fromKeys(keyPath));
        result.setValueDef(CommonProperties.<PlatformOrder>getIdentityValueDef());
        result.setKeyEncodingDef(PlatformOrderKeyEncodingDef.INSTANCE);
        return result.create();
    }

    private enum PlatformOrderKeyEncodingDef implements PropertyKeyEncodingDef<PlatformOrder> {
        INSTANCE;

        @Override
        public PlatformOrder decode(ConfigTree config) {
            List<ConfigTree> platformIds = config.getChildTrees("platform-id");
            if (platformIds.isEmpty()) {
                return null;
            }

            List<String> result = new ArrayList<>(platformIds.size());
            for (ConfigTree platformId: platformIds) {
                result.add(platformId.getValue(""));
            }
            return PlatformOrder.fromPlatformIds(result);
        }

        @Override
        public ConfigTree encode(PlatformOrder value) {
            ConfigTree.Builder result = new ConfigTree.Builder();
            for (String id: value.getPlatformIds()) {
                result.addChildBuilder("platform-id").setValue(id);
            }
            return result.create();
        }
    }

    private static class DefaultHolder {
        private static final ActiveSettingsQuery DEFAULT_ACTIVE_SETTINGS = loadDefaultActiveSettings();
        private static final CommonGlobalSettings DEFAULT_STORED = new CommonGlobalSettings(DEFAULT_ACTIVE_SETTINGS);
        private static volatile CommonGlobalSettings DEFAULT = DEFAULT_STORED;

        public static void withCleanMemorySettings(NbConsumer<? super GenericProfileSettings> task) {
            ExceptionHelper.checkNotNullArgument(task, "task");
            try {
                GenericProfileSettings settings = GenericProfileSettings.createTestMemorySettings();
                DEFAULT = new CommonGlobalSettings(new MultiProfileProperties(Collections.<SingleProfileSettingsEx>singletonList(settings)));
                task.accept(settings);
            } finally {
                DEFAULT = DEFAULT_STORED;
            }
        }
    }
}
