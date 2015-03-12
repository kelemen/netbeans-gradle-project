package org.netbeans.gradle.project.properties2;

import java.nio.charset.Charset;
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
        builtInTasks = get(BuiltInTasksProperty.PROPERTY_DEF, activeSettingsQuery);
        commonTasks = get(CommonTasksProperty.PROPERTY_DEF, activeSettingsQuery);
        customTasks = get(CustomTasksProperty.PROPERTY_DEF, activeSettingsQuery);
        gradleLocation = get(GradleLocationProperty.PROPERTY_DEF, activeSettingsQuery);
        licenseHeaderInfo = get(LicenseHeaderInfoProperty.PROPERTY_DEF, activeSettingsQuery);
        scriptPlatform = get(ScriptPlatformProperty.PROPERTY_DEF, activeSettingsQuery);
        sourceEncoding = get(SourceEncodingProperty.PROPERTY_DEF, activeSettingsQuery);
        targetPlatform = get(TargetPlatformProperty.PROPERTY_DEF, activeSettingsQuery);
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
            PropertyDef<?, ValueType> propertyDef,
            ActiveSettingsQuery activeSettingsQuery) {

        return new PropertyReference<>(propertyDef, activeSettingsQuery);
    }
}
