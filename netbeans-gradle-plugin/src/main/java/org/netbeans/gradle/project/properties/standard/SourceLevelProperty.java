package org.netbeans.gradle.project.properties.standard;

import java.util.Objects;
import java.util.regex.Pattern;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.property.PropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.query.J2SEPlatformFromScriptQuery;
import org.netbeans.gradle.project.util.CachedLookupValue;
import org.openide.modules.SpecificationVersion;

public final class SourceLevelProperty {
    public static final String DEFAULT_SOURCE_LEVEL = "1.7";

    private static final ConfigPath CONFIG_ROOT = ConfigPath.fromKeys("source-level");

    public static final PropertyDef<String, String> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<String, String> createPropertyDef() {
        PropertyDef.Builder<String, String> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);
        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setValueMerger(CommonProperties.<String>getParentIfNullValueMerger());
        return result.create();
    }

    public static PropertySource<String> defaultValue(
            final NbGradleProject project,
            final PropertySource<? extends ProjectPlatform> targetPlatform) {
        Objects.requireNonNull(project, "project");
        Objects.requireNonNull(targetPlatform, "targetPlatform");

        final CachedLookupValue<JavaExtension> javaExtRef
                = new CachedLookupValue<>(project, JavaExtension.class);

        return new PropertySource<String>() {
            @Override
            public String getValue() {
                if (isReliableJavaVersion(javaExtRef.get())) {
                    String sourceLevel = tryGetScriptSourceLevel(project);
                    if (sourceLevel != null) {
                        return sourceLevel;
                    }
                }
                return getSourceLevelFromPlatform(targetPlatform.getValue());
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                Objects.requireNonNull(listener, "listener");

                ListenerRef ref1 = project.currentModel().addChangeListener(listener);
                ListenerRef ref2 = targetPlatform.addChangeListener(listener);

                return ListenerRefs.combineListenerRefs(ref1, ref2);
            }
        };
    }

    public static String getSourceLevelFromPlatform(JavaPlatform platform) {
        SpecificationVersion version = platform.getSpecification().getVersion();
        String versionStr = version != null ? version.toString() : "";
        return getSourceLevelFromPlatformVersion(versionStr);
    }

    public static String getSourceLevelFromPlatform(ProjectPlatform platform) {
        return getSourceLevelFromPlatformVersion(platform.getVersion());
    }

    private static String getSourceLevelFromPlatformVersion(String version) {
        String[] versionParts = version.split(Pattern.quote("."));
        if (versionParts.length < 2) {
            return DEFAULT_SOURCE_LEVEL;
        }
        else {
            return versionParts[0] + "." + versionParts[1];
        }
    }

    public static J2SEPlatformFromScriptQuery tryGetPlatformScriptQuery(NbGradleProject project) {
        return project.getExtensions().lookupExtensionObj(J2SEPlatformFromScriptQuery.class);
    }

    private static String tryGetScriptSourceLevel(NbGradleProject project) {
        J2SEPlatformFromScriptQuery query = tryGetPlatformScriptQuery(project);
        return query != null ? query.getSourceLevel() : null;
    }

    public static boolean isReliableJavaVersion(JavaExtension javaExt) {
        return javaExt != null;
    }

    private SourceLevelProperty() {
        throw new AssertionError();
    }
}
