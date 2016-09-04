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
import org.netbeans.gradle.project.properties.global.CommonGlobalSettings;
import org.netbeans.gradle.project.properties.standard.BuiltInTasks;
import org.netbeans.gradle.project.properties.standard.BuiltInTasksProperty;
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

    public NbGradleCommonProperties(NbGradleProject ownerProject, ActiveSettingsQuery activeSettingsQuery) {
        ExceptionHelper.checkNotNullArgument(ownerProject, "ownerProject");
        ExceptionHelper.checkNotNullArgument(activeSettingsQuery, "activeSettingsQuery");

        this.ownerProject = ownerProject;
        this.activeSettingsQuery = activeSettingsQuery;

        // Warning: "get" uses the fields set above.
        CommonGlobalSettings globalSettings = CommonGlobalSettings.getDefault();

        builtInTasks = builtInTasks(ownerProject, activeSettingsQuery);
        customTasks = customTasks(activeSettingsQuery);
        gradleLocation = get(
                GradleLocationProperty.PROPERTY_DEF,
                PropertyFactory.constSource(GradleLocationDef.DEFAULT));
        licenseHeaderInfo = licenseHeaderInfo(activeSettingsQuery);
        scriptPlatform = get(
                ScriptPlatformProperty.getPropertyDef(globalSettings.platformPreferenceOrder()),
                PropertyFactory.constSource(ScriptPlatform.getDefault()));
        sourceEncoding = get(
                SourceEncodingProperty.PROPERTY_DEF,
                PropertyFactory.constSource(SourceEncodingProperty.DEFAULT_SOURCE_ENCODING));
        targetPlatform = get(TargetPlatformProperty.PROPERTY_DEF, TargetPlatformProperty.defaultValue(ownerProject));
        sourceLevel = get(
                SourceLevelProperty.PROPERTY_DEF,
                SourceLevelProperty.defaultValue(ownerProject, targetPlatform.getActiveSource()));
        userInitScriptPath = new PropertyReference<>(UserInitScriptProperty.PROPERTY_DEF, activeSettingsQuery);
        displayNamePattern = displayNamePattern(activeSettingsQuery);
        customVariables = customVariables(activeSettingsQuery);
    }

    public static PropertyReference<LicenseHeaderInfo> licenseHeaderInfo(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                LicenseHeaderInfoProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.<LicenseHeaderInfo>constSource(null));
    }

    public static PropertyReference<BuiltInTasks> builtInTasks(
            NbGradleProject ownerProject,
            ActiveSettingsQuery activeSettingsQuery) {

        return new PropertyReference<>(
                BuiltInTasksProperty.PROPERTY_DEF,
                activeSettingsQuery,
                BuiltInTasksProperty.defaultValue(ownerProject, activeSettingsQuery));
    }

    public static PropertyReference<PredefinedTasks> customTasks(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                CustomTasksProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(PredefinedTasks.NO_TASKS));
    }

    public static PropertyReference<String> displayNamePattern(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                ProjectDisplayNameProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(ProjectDisplayNameProperty.DEFAULT_VALUE));
    }

    public static PropertyReference<CustomVariables> customVariables(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                CustomVariablesProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.constSource(MemCustomVariables.EMPTY));
    }

    public Project getOwnerProject() {
        return ownerProject;
    }

    public ActiveSettingsQuery getActiveSettingsQuery() {
        return activeSettingsQuery;
    }

    public PropertyReference<BuiltInTasks> builtInTasks() {
        return builtInTasks;
    }

    public PropertyReference<PredefinedTasks> customTasks() {
        return customTasks;
    }

    public PropertyReference<GradleLocationDef> gradleLocation() {
        return gradleLocation;
    }

    public PropertyReference<LicenseHeaderInfo> licenseHeaderInfo() {
        return licenseHeaderInfo;
    }

    public PropertyReference<ScriptPlatform> scriptPlatform() {
        return scriptPlatform;
    }

    public PropertyReference<Charset> sourceEncoding() {
        return sourceEncoding;
    }

    public PropertyReference<ProjectPlatform> targetPlatform() {
        return targetPlatform;
    }

    public PropertyReference<String> sourceLevel() {
        return sourceLevel;
    }

    public PropertyReference<UserInitScriptPath> userInitScriptPath() {
        return userInitScriptPath;
    }

    public PropertyReference<String> displayNamePattern() {
        return displayNamePattern;
    }

    public PropertyReference<CustomVariables> customVariables() {
        return customVariables;
    }

    private <ValueType> PropertyReference<ValueType> get(
            PropertyDef<?, ValueType> propertyDef,
            PropertySource<? extends ValueType> defaultValue) {

        return new PropertyReference<>(propertyDef, activeSettingsQuery, defaultValue);
    }
}
