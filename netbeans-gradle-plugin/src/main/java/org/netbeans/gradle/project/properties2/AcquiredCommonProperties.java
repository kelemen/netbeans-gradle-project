package org.netbeans.gradle.project.properties2;

import java.nio.charset.Charset;
import org.jtrim.property.PropertyFactory;
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
        builtInTasks = acquire(activeSettingsQuery, BuiltInTasksProperty.PROPERTY_DEF);
        commonTasks = acquire(activeSettingsQuery, CommonTasksProperty.PROPERTY_DEF);
        customTasks = acquire(activeSettingsQuery, CustomTasksProperty.PROPERTY_DEF);
        gradleLocation = acquire(activeSettingsQuery, GradleLocationProperty.PROPERTY_DEF);
        licenseHeaderInfo = acquire(activeSettingsQuery, LicenseHeaderInfoProperty.PROPERTY_DEF);
        scriptPlatform = acquire(activeSettingsQuery, ScriptPlatformProperty.PROPERTY_DEF);
        sourceEncoding = acquire(activeSettingsQuery, SourceEncodingProperty.PROPERTY_DEF);
        targetPlatform = acquire(activeSettingsQuery, TargetPlatformProperty.PROPERTY_DEF);
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

    private <ValueType> PropertySource<ValueType> acquire(
            ActiveSettingsQuery activeSettingsQuery,
            PropertyDef<?, ValueType> propertyDef) {

        AcquiredPropertySource<ValueType> result = activeSettingsQuery.acquireProperty(propertyDef);
        return PropertyFactory.protectedView(result);
    }
}
