package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ActiveSettingsQuery;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyReference;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.license.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.netbeans.gradle.project.properties.standard.BuiltInTasks;
import org.netbeans.gradle.project.properties.standard.BuiltInTasksProperty;
import org.netbeans.gradle.project.properties.standard.CommonProperties;
import org.netbeans.gradle.project.properties.standard.CustomTasksProperty;
import org.netbeans.gradle.project.properties.standard.CustomVariables;
import org.netbeans.gradle.project.properties.standard.CustomVariablesProperty;
import org.netbeans.gradle.project.properties.standard.GradleLocationProperty;
import org.netbeans.gradle.project.properties.standard.LicenseHeaderInfoProperty;
import org.netbeans.gradle.project.properties.standard.MemCustomVariables;
import org.netbeans.gradle.project.properties.standard.PredefinedTasks;
import org.netbeans.gradle.project.properties.standard.ProjectDisplayNameProperty;
import org.netbeans.gradle.project.properties.standard.ScriptPlatformProperty;
import org.netbeans.gradle.project.properties.standard.SourceEncodingProperty;
import org.netbeans.gradle.project.properties.standard.SourceLevelProperty;
import org.netbeans.gradle.project.properties.standard.TargetPlatformProperty;
import org.netbeans.gradle.project.properties.standard.UserInitScriptPath;
import org.netbeans.gradle.project.properties.standard.UserInitScriptProperty;


public final class NbGradleCommonProperties {
    private final NbGradleProject ownerProject;
    private final ActiveSettingsQuery activeSettingsQuery;

    private final PropertyReference<BuiltInTasks> builtInTasks;
    private final PropertyReference<PredefinedTasks> customTasks;
    private final PropertyReference<GradleLocationDef> gradleLocation;
    private final PropertyReference<LicenseHeaderInfo> licenseHeaderInfo;
    private final PropertyReference<ScriptPlatform> scriptPlatform;
    private final PropertyReference<Charset> sourceEncoding;
    private final PropertyReference<ProjectPlatform> targetPlatform;
    private final PropertyReference<String> sourceLevel;
    private final PropertyReference<UserInitScriptPath> userInitScriptPath;
    private final PropertyReference<String> displayNamePattern;
    private final PropertyReference<CustomVariables> customVariables;
    private final PropertyReference<Boolean> showGradleVersion;

    public NbGradleCommonProperties(NbGradleProject ownerProject, ActiveSettingsQuery activeSettingsQuery) {
        ExceptionHelper.checkNotNullArgument(ownerProject, "ownerProject");
        ExceptionHelper.checkNotNullArgument(activeSettingsQuery, "activeSettingsQuery");

        this.ownerProject = ownerProject;
        this.activeSettingsQuery = activeSettingsQuery;

        this.builtInTasks = builtInTasks(ownerProject, activeSettingsQuery);
        this.customTasks = customTasks(activeSettingsQuery);
        this.gradleLocation = gradleLocation(activeSettingsQuery);
        this.licenseHeaderInfo = licenseHeaderInfo(activeSettingsQuery);
        this.scriptPlatform = scriptPlatform(activeSettingsQuery);
        this.sourceEncoding = sourceEncoding(activeSettingsQuery);
        this.userInitScriptPath = userInitScriptPath(activeSettingsQuery);
        this.targetPlatform = targetPlatform(ownerProject, activeSettingsQuery);
        this.sourceLevel = sourceLevel(ownerProject, activeSettingsQuery, this.targetPlatform.getActiveSource());
        this.displayNamePattern = displayNamePattern(activeSettingsQuery);
        this.customVariables = customVariables(activeSettingsQuery);
        this.showGradleVersion = showGradleVersion(activeSettingsQuery);
    }

    public Project getOwnerProject() {
        return ownerProject;
    }

    public ActiveSettingsQuery getActiveSettingsQuery() {
        return activeSettingsQuery;
    }

    public static PropertyReference<BuiltInTasks> builtInTasks(
            NbGradleProject ownerProject,
            ActiveSettingsQuery activeSettingsQuery) {

        return get(
                BuiltInTasksProperty.PROPERTY_DEF,
                activeSettingsQuery,
                BuiltInTasksProperty.defaultValue(ownerProject, activeSettingsQuery));
    }

    public PropertyReference<BuiltInTasks> builtInTasks() {
        return builtInTasks;
    }

    public static PropertyReference<PredefinedTasks> customTasks(ActiveSettingsQuery activeSettingsQuery) {
        return get(
                CustomTasksProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(PredefinedTasks.NO_TASKS));
    }

    public PropertyReference<PredefinedTasks> customTasks() {
        return customTasks;
    }

    public static PropertyReference<GradleLocationDef> gradleLocation(ActiveSettingsQuery activeSettingsQuery) {
        return get(
                GradleLocationProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(GradleLocationDef.DEFAULT));
    }

    public PropertyReference<GradleLocationDef> gradleLocation() {
        return gradleLocation;
    }

    public static PropertyReference<LicenseHeaderInfo> licenseHeaderInfo(ActiveSettingsQuery activeSettingsQuery) {
        return get(
                LicenseHeaderInfoProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.<LicenseHeaderInfo>constSource(null));
    }

    public PropertyReference<LicenseHeaderInfo> licenseHeaderInfo() {
        return licenseHeaderInfo;
    }

    public static PropertyReference<ScriptPlatform> scriptPlatform(ActiveSettingsQuery activeSettingsQuery) {
        return scriptPlatform(
                activeSettingsQuery,
                CommonGlobalSettings.getDefault().platformPreferenceOrder().getActiveSource());
    }

    public static PropertyReference<ScriptPlatform> scriptPlatform(
            ActiveSettingsQuery activeSettingsQuery,
            PropertySource<? extends PlatformOrder> platformPreferenceOrder) {
        return get(
                ScriptPlatformProperty.getPropertyDef(platformPreferenceOrder),
                activeSettingsQuery,
                PropertyFactory.constSource(ScriptPlatform.getDefault()));
    }

    public PropertyReference<ScriptPlatform> scriptPlatform() {
        return scriptPlatform;
    }

    public static PropertyReference<Charset> sourceEncoding(ActiveSettingsQuery activeSettingsQuery) {
        return get(
                SourceEncodingProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(SourceEncodingProperty.DEFAULT_SOURCE_ENCODING));
    }

    public PropertyReference<Charset> sourceEncoding() {
        return sourceEncoding;
    }

    public static PropertyReference<ProjectPlatform> targetPlatform(
            NbGradleProject ownerProject,
            ActiveSettingsQuery activeSettingsQuery) {
        return get(
                TargetPlatformProperty.PROPERTY_DEF,
                activeSettingsQuery,
                TargetPlatformProperty.defaultValue(ownerProject));
    }

    public PropertyReference<ProjectPlatform> targetPlatform() {
        return targetPlatform;
    }

    public static PropertyReference<String> sourceLevel(
            NbGradleProject ownerProject,
            ActiveSettingsQuery activeSettingsQuery) {
        return sourceLevel(
                ownerProject,
                activeSettingsQuery,
                targetPlatform(ownerProject, activeSettingsQuery).getActiveSource());
    }

    private static PropertyReference<String> sourceLevel(
            NbGradleProject ownerProject,
            ActiveSettingsQuery activeSettingsQuery,
            PropertySource<? extends ProjectPlatform> targetPlatform) {
        return get(
                SourceLevelProperty.PROPERTY_DEF,
                activeSettingsQuery,
                SourceLevelProperty.defaultValue(ownerProject, targetPlatform));
    }

    public PropertyReference<String> sourceLevel() {
        return sourceLevel;
    }

    public static PropertyReference<UserInitScriptPath> userInitScriptPath(ActiveSettingsQuery activeSettingsQuery) {
        return get(
                UserInitScriptProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.<UserInitScriptPath>constSource(null));
    }

    public PropertyReference<UserInitScriptPath> userInitScriptPath() {
        return userInitScriptPath;
    }

    public static PropertyReference<String> displayNamePattern(ActiveSettingsQuery activeSettingsQuery) {
        return get(
                ProjectDisplayNameProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(ProjectDisplayNameProperty.DEFAULT_VALUE));
    }

    public PropertyReference<String> displayNamePattern() {
        return displayNamePattern;
    }

    public static PropertyReference<CustomVariables> customVariables(ActiveSettingsQuery activeSettingsQuery) {
        return get(
                CustomVariablesProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(MemCustomVariables.EMPTY));
    }

    public PropertyReference<CustomVariables> customVariables() {
        return customVariables;
    }

    public static PropertyReference<Boolean> showGradleVersion(ActiveSettingsQuery activeSettingsQuery) {
        return get(
                CommonProperties.defineBooleanProperty("tasks", "show-gradle-version"),
                activeSettingsQuery,
                PropertyFactory.constSource(false));
    }

    public PropertyReference<Boolean> showGradleVersion() {
        return showGradleVersion;
    }

    private static <ValueType> PropertyReference<ValueType> get(
            PropertyDef<?, ValueType> propertyDef,
            ActiveSettingsQuery activeSettingsQuery,
            PropertySource<? extends ValueType> defaultValue) {

        return new PropertyReference<>(propertyDef, activeSettingsQuery, defaultValue);
    }
}
