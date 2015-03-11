package org.netbeans.gradle.project.properties2;

import java.nio.charset.Charset;
import org.jtrim.property.PropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties2.standard.BuiltInTasks;
import org.netbeans.gradle.project.properties2.standard.BuiltInTasksProperty;
import org.netbeans.gradle.project.properties2.standard.CommonTasksProperty;
import org.netbeans.gradle.project.properties2.standard.CustomTasks;
import org.netbeans.gradle.project.properties2.standard.CustomTasksProperty;
import org.netbeans.gradle.project.properties2.standard.GradleLocationProperty;
import org.netbeans.gradle.project.properties2.standard.LicenseHeaderInfoProperty;
import org.netbeans.gradle.project.properties2.standard.PredefinedTasks;
import org.netbeans.gradle.project.properties2.standard.ScriptPlatformProperty;
import org.netbeans.gradle.project.properties2.standard.SourceEncodingProperty;
import org.netbeans.gradle.project.properties2.standard.TargetPlatformProperty;

public final class AcquiredCommonProperties {
    private final PropertySource<BuiltInTasks> builtInTasks;
    private final PropertySource<PredefinedTasks> commonTasks;
    private final PropertySource<CustomTasks> customTasks;
    private final PropertySource<GradleLocation> gradleLocation;
    private final PropertySource<LicenseHeaderInfo> licenseHeaderInfo;
    private final PropertySource<JavaPlatform> scriptPlatform;
    private final PropertySource<Charset> sourceEncoding;
    private final PropertySource<ProjectPlatform> targetPlatform;

    public AcquiredCommonProperties(ActiveSettingsQuery activeSettingsQuery) {
        builtInTasks = get(activeSettingsQuery, BuiltInTasksProperty.PROPERTY_DEF);
        commonTasks = get(activeSettingsQuery, CommonTasksProperty.PROPERTY_DEF);
        customTasks = get(activeSettingsQuery, CustomTasksProperty.PROPERTY_DEF);
        gradleLocation = get(activeSettingsQuery, GradleLocationProperty.PROPERTY_DEF);
        licenseHeaderInfo = get(activeSettingsQuery, LicenseHeaderInfoProperty.PROPERTY_DEF);
        scriptPlatform = get(activeSettingsQuery, ScriptPlatformProperty.PROPERTY_DEF);
        sourceEncoding = get(activeSettingsQuery, SourceEncodingProperty.PROPERTY_DEF);
        targetPlatform = get(activeSettingsQuery, TargetPlatformProperty.PROPERTY_DEF);
    }

    public PropertySource<BuiltInTasks> getBuiltInTasks() {
        return builtInTasks;
    }

    public PropertySource<PredefinedTasks> getCommonTasks() {
        return commonTasks;
    }

    public PropertySource<CustomTasks> getCustomTasks() {
        return customTasks;
    }

    public PropertySource<GradleLocation> getGradleLocation() {
        return gradleLocation;
    }

    public PropertySource<LicenseHeaderInfo> getLicenseHeaderInfo() {
        return licenseHeaderInfo;
    }

    public PropertySource<JavaPlatform> getScriptPlatform() {
        return scriptPlatform;
    }

    public PropertySource<Charset> getSourceEncoding() {
        return sourceEncoding;
    }

    public PropertySource<ProjectPlatform> getTargetPlatform() {
        return targetPlatform;
    }

    private static <ValueType> PropertySource<ValueType> get(
            ActiveSettingsQuery activeSettingsQuery,
            PropertyDef<?, ValueType> propertyDef) {

        return activeSettingsQuery.getProperty(propertyDef);
    }
}
