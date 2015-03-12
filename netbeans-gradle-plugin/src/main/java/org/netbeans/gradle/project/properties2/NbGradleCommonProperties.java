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

public final class NbGradleCommonProperties {
    private final PropertyReference<BuiltInTasks> builtInTasks;
    private final PropertyReference<PredefinedTasks> commonTasks;
    private final PropertyReference<CustomTasks> customTasks;
    private final PropertyReference<GradleLocation> gradleLocation;
    private final PropertyReference<LicenseHeaderInfo> licenseHeaderInfo;
    private final PropertyReference<JavaPlatform> scriptPlatform;
    private final PropertyReference<Charset> sourceEncoding;
    private final PropertyReference<ProjectPlatform> targetPlatform;

    public NbGradleCommonProperties(ActiveSettingsQuery activeSettingsQuery) {
        builtInTasks = get(activeSettingsQuery, BuiltInTasksProperty.PROPERTY_DEF);
        commonTasks = get(activeSettingsQuery, CommonTasksProperty.PROPERTY_DEF);
        customTasks = get(activeSettingsQuery, CustomTasksProperty.PROPERTY_DEF);
        gradleLocation = get(activeSettingsQuery, GradleLocationProperty.PROPERTY_DEF);
        licenseHeaderInfo = get(activeSettingsQuery, LicenseHeaderInfoProperty.PROPERTY_DEF);
        scriptPlatform = get(activeSettingsQuery, ScriptPlatformProperty.PROPERTY_DEF);
        sourceEncoding = get(activeSettingsQuery, SourceEncodingProperty.PROPERTY_DEF);
        targetPlatform = get(activeSettingsQuery, TargetPlatformProperty.PROPERTY_DEF);
    }

    public PropertyReference<BuiltInTasks> builtInTasks() {
        return builtInTasks;
    }

    public PropertyReference<PredefinedTasks> commonTasks() {
        return commonTasks;
    }

    public PropertyReference<CustomTasks> customTasks() {
        return customTasks;
    }

    public PropertyReference<GradleLocation> gradleLocation() {
        return gradleLocation;
    }

    public PropertyReference<LicenseHeaderInfo> licenseHeaderInfo() {
        return licenseHeaderInfo;
    }

    public PropertyReference<JavaPlatform> scriptPlatform() {
        return scriptPlatform;
    }

    public PropertyReference<Charset> sourceEncoding() {
        return sourceEncoding;
    }

    public PropertyReference<ProjectPlatform> targetPlatform() {
        return targetPlatform;
    }

    private static <ValueType> PropertyReference<ValueType> get(
            ActiveSettingsQuery activeSettingsQuery,
            PropertyDef<?, ValueType> propertyDef) {

        return new PropertyReference<>(propertyDef, activeSettingsQuery);
    }
}
