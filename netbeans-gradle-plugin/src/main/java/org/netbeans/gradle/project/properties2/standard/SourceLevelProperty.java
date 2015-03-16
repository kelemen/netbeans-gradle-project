package org.netbeans.gradle.project.properties2.standard;

import java.util.regex.Pattern;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.query.J2SEPlatformFromScriptQuery;
import org.netbeans.gradle.project.util.CachedLookupValue;
import org.openide.modules.SpecificationVersion;

public final class SourceLevelProperty {
    public static final String DEFAULT_SOURCE_LEVEL = "1.7";

    private static final String CONFIG_KEY_SOURCE_LEVEL = "source-level";

    public static final PropertyDef<String, String> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<String, String> createPropertyDef() {
        PropertyDef.Builder<String, String> result
                = new PropertyDef.Builder<>(ConfigPath.fromKeys(CONFIG_KEY_SOURCE_LEVEL));
        result.setKeyEncodingDef(CommonProperties.getIdentityKeyEncodingDef());
        result.setValueDef(CommonProperties.<String>getIdentityValueDef());
        result.setValueMerger(CommonProperties.<String>getParentIfNullValueMerger());
        return result.create();
    }

    public static PropertySource<String> defaultValue(
            final NbGradleProject project,
            final PropertySource<ProjectPlatform> targetPlatform) {
        ExceptionHelper.checkNotNullArgument(project, "project");
        ExceptionHelper.checkNotNullArgument(targetPlatform, "targetPlatform");

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
                ExceptionHelper.checkNotNullArgument(listener, "listener");

                ListenerRef ref1 = GlobalGradleSettings.getMayRelyOnJavaOfScript().addChangeListener(listener);
                ListenerRef ref2 = project.addModelChangeListener(listener);
                ListenerRef ref3 = targetPlatform.addChangeListener(listener);

                return ListenerRegistries.combineListenerRefs(ref1, ref2, ref3);
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
        return project.getCombinedExtensionLookup().lookup(J2SEPlatformFromScriptQuery.class);
    }

    private static String tryGetScriptSourceLevel(NbGradleProject project) {
        J2SEPlatformFromScriptQuery query = tryGetPlatformScriptQuery(project);
        return query != null ? query.getSourceLevel() : null;
    }

    private static boolean isModelSourceReliable(JavaExtension javaExt) {
        return javaExt.getCurrentModel().getModelSource().isReliableJavaVersion();
    }

    public static boolean isReliableJavaVersion(JavaExtension javaExt) {
        return (javaExt != null && isModelSourceReliable(javaExt))
                || GlobalGradleSettings.getMayRelyOnJavaOfScript().getValue();
    }

    private SourceLevelProperty() {
        throw new AssertionError();
    }
}
