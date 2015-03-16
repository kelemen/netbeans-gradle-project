package org.netbeans.gradle.project.properties2;

import java.nio.charset.Charset;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.event.ListenerRef;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.GradleLocation;
import org.netbeans.gradle.project.properties.LicenseHeaderInfo;
import org.netbeans.gradle.project.properties2.standard.BuiltInTasks;
import org.netbeans.gradle.project.properties2.standard.BuiltInTasksProperty;
import org.netbeans.gradle.project.properties2.standard.CustomTasksProperty;
import org.netbeans.gradle.project.properties2.standard.GradleLocationProperty;
import org.netbeans.gradle.project.properties2.standard.LicenseHeaderInfoProperty;
import org.netbeans.gradle.project.properties2.standard.PredefinedTasks;
import org.netbeans.gradle.project.properties2.standard.ScriptPlatformProperty;
import org.netbeans.gradle.project.properties2.standard.SourceEncodingProperty;
import org.netbeans.gradle.project.properties2.standard.SourceLevelProperty;
import org.netbeans.gradle.project.properties2.standard.TargetPlatformProperty;

public final class NbGradleCommonProperties {
    private final NbGradleProject ownerProject;
    private final ActiveSettingsQuery activeSettingsQuery;

    private final PropertyReference<BuiltInTasks> builtInTasks;
    private final PropertyReference<PredefinedTasks> customTasks;
    private final PropertyReference<GradleLocation> gradleLocation;
    private final PropertyReference<LicenseHeaderInfo> licenseHeaderInfo;
    private final PropertyReference<JavaPlatform> scriptPlatform;
    private final PropertyReference<Charset> sourceEncoding;
    private final PropertyReference<ProjectPlatform> targetPlatform;
    private final PropertyReference<String> sourceLevel;

    public NbGradleCommonProperties(NbGradleProject ownerProject, ActiveSettingsQuery activeSettingsQuery) {
        ExceptionHelper.checkNotNullArgument(ownerProject, "ownerProject");
        ExceptionHelper.checkNotNullArgument(activeSettingsQuery, "activeSettingsQuery");

        this.ownerProject = ownerProject;
        this.activeSettingsQuery = activeSettingsQuery;

        // Warning: "get" uses the fields set above.

        builtInTasks = get(BuiltInTasksProperty.PROPERTY_DEF, BuiltInTasksProperty.defaultValue(ownerProject, activeSettingsQuery));
        customTasks = get(CustomTasksProperty.PROPERTY_DEF, PropertyFactory.constSource(PredefinedTasks.NO_TASKS));
        gradleLocation = get(GradleLocationProperty.PROPERTY_DEF, GlobalGradleSettings.getGradleHome());
        licenseHeaderInfo = licenseHeaderInfo(activeSettingsQuery);
        scriptPlatform = get(ScriptPlatformProperty.PROPERTY_DEF, GlobalGradleSettings.getGradleJdk());
        sourceEncoding = get(SourceEncodingProperty.PROPERTY_DEF, PropertyFactory.constSource(SourceEncodingProperty.DEFAULT_SOURCE_ENCODING));
        targetPlatform = get(TargetPlatformProperty.PROPERTY_DEF, TargetPlatformProperty.defaultValue(ownerProject));
        sourceLevel = get(SourceLevelProperty.PROPERTY_DEF, SourceLevelProperty.defaultValue(ownerProject, targetPlatform.getActiveSource()));
    }

    public static PropertyReference<LicenseHeaderInfo> licenseHeaderInfo(ActiveSettingsQuery activeSettingsQuery) {
        return new PropertyReference<>(
                LicenseHeaderInfoProperty.PROPERTY_DEF,
                activeSettingsQuery,
                PropertyFactory.<LicenseHeaderInfo>constSource(null));
    }

    public Project getOwnerProject() {
        return ownerProject;
    }

    public ActiveSettingsQuery getActiveSettingsQuery() {
        return activeSettingsQuery;
    }

    public void waitForLoadedOnce(CancellationToken cancelToken) {
        activeSettingsQuery.waitForLoadedOnce(cancelToken);
    }

    public ListenerRef afterLoaded(Runnable listener) {
        return activeSettingsQuery.notifyWhenLoadedOnce(listener);
    }

    public PropertyReference<BuiltInTasks> builtInTasks() {
        return builtInTasks;
    }

    public PropertyReference<PredefinedTasks> customTasks() {
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

    public PropertyReference<String> sourceLevel() {
        return sourceLevel;
    }

    private <ValueType> PropertyReference<ValueType> get(
            PropertyDef<?, ValueType> propertyDef,
            PropertySource<? extends ValueType> defaultValue) {

        return new PropertyReference<>(propertyDef, activeSettingsQuery, defaultValue);
    }
}
