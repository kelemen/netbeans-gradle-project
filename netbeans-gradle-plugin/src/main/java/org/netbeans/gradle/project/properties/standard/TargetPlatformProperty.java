package org.netbeans.gradle.project.properties.standard;

import java.util.Arrays;
import java.util.List;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.properties.GlobalGradleSettings;
import org.netbeans.gradle.project.properties.ConfigPath;
import org.netbeans.gradle.project.properties.ConfigTree;
import org.netbeans.gradle.project.properties.PropertyDef;
import org.netbeans.gradle.project.properties.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.properties.PropertyValueDef;
import org.netbeans.gradle.project.query.J2SEPlatformFromScriptQuery;
import org.netbeans.gradle.project.util.CachedLookupValue;
import org.openide.util.Lookup;

public final class TargetPlatformProperty {
    private static final String CONFIG_KEY_PLATFORM_NAME = "target-platform-name";
    private static final String CONFIG_KEY_PLATFORM_VERSION = "target-platform";

    public static final PropertyDef<PlatformId, ProjectPlatform> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<PlatformId, ProjectPlatform> createPropertyDef() {
        List<ConfigPath> paths = Arrays.asList(
                ConfigPath.fromKeys(CONFIG_KEY_PLATFORM_NAME),
                ConfigPath.fromKeys(CONFIG_KEY_PLATFORM_VERSION));

        PropertyDef.Builder<PlatformId, ProjectPlatform> result
                = new PropertyDef.Builder<>(paths);

        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(getValueDef());
        return result.create();
    }

    public static PropertySource<ProjectPlatform> defaultValue(
            final NbGradleProject project) {

        final CachedLookupValue<JavaExtension> javaExtRef
                = new CachedLookupValue<>(project, JavaExtension.class);

        // This is here only to register and remove listeners because
        // it can detect changes in the list of platforms defined in
        // NetBeans. We will never request the value of this property
        // source, so the actual parameters do not matter.
        final PropertySource<?> platformListHelper
                = JavaPlatformUtils.findPlatformSource("j2se", "1.3");
        return new PropertySource<ProjectPlatform>() {
            @Override
            public ProjectPlatform getValue() {
                if (SourceLevelProperty.isReliableJavaVersion(javaExtRef.get())) {
                    ProjectPlatform platform = tryGetScriptPlatform(project);
                    if (platform != null) {
                        return platform;
                    }
                }
                return JavaPlatformUtils.getDefaultPlatform();
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                ExceptionHelper.checkNotNullArgument(listener, "listener");

                ListenerRef ref1 = GlobalGradleSettings.getMayRelyOnJavaOfScript().addChangeListener(listener);
                ListenerRef ref2 = project.addModelChangeListener(listener);
                ListenerRef ref3 = platformListHelper.addChangeListener(listener);

                return ListenerRegistries.combineListenerRefs(ref1, ref2, ref3);
            }
        };
    }

    private static ProjectPlatform tryGetScriptPlatform(NbGradleProject project) {
        J2SEPlatformFromScriptQuery query = SourceLevelProperty.tryGetPlatformScriptQuery(project);
        return query != null ? query.getPlatform(): null;
    }

    private static GradleProjectPlatformQuery findOwnerQuery(String name) {
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            if (query.isOwnerQuery(name)) {
                return query;
            }
        }
        return null;
    }

    private static GradleProjectPlatformQuery findQueryFromAll(PlatformId id) {
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            ProjectPlatform platform = query.tryFindPlatformByName(id.getName(), id.getVersion());
            if (platform != null) {
                return query;
            }
        }
        return null;
    }

    private static PropertySource<ProjectPlatform> getProjectPlatform(final PlatformId id) {
        assert id != null;

        GradleProjectPlatformQuery query = findOwnerQuery(id.getName());
        if (query == null) {
            // To be really safe, we should listen for changes in all queries
            // in this case. However, that is inefficient and this should not
            // happen when platform queries are properly implemented.
            query = findQueryFromAll(id);
        }
        if (query == null) {
            return PropertyFactory.constSource(null);
        }

        final GradleProjectPlatformQuery choosenQuery = query;
        return new PropertySource<ProjectPlatform>() {
            @Override
            public ProjectPlatform getValue() {
                return choosenQuery.tryFindPlatformByName(id.getName(), id.getVersion());
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                return choosenQuery.addPlatformChangeListener(listener);
            }
        };
    }

    private static PropertyValueDef<PlatformId, ProjectPlatform> getValueDef() {
        return new PropertyValueDef<PlatformId, ProjectPlatform>() {
            @Override
            public PropertySource<ProjectPlatform> property(PlatformId valueKey) {
                return valueKey != null
                        ? getProjectPlatform(valueKey)
                        : PropertyFactory.<ProjectPlatform>constSource(null);
            }

            @Override
            public PlatformId getKeyFromValue(ProjectPlatform value) {
                if (value == null) {
                    return null;
                }

                String name = value.getName();
                String version = value.getVersion();
                return new PlatformId(name, version);
            }
        };
    }

    private static PropertyKeyEncodingDef<PlatformId> getEncodingDef() {
        return new PropertyKeyEncodingDef<PlatformId>() {
            @Override
            public PlatformId decode(ConfigTree config) {
                ConfigTree name = config.getChildTree(CONFIG_KEY_PLATFORM_NAME);
                ConfigTree version = config.getChildTree(CONFIG_KEY_PLATFORM_VERSION);

                String versionStr = version.getValue(null);
                if (versionStr == null) {
                    return null;
                }
                return new PlatformId(name.getValue(PlatformId.DEFAULT_NAME), versionStr);
            }

            @Override
            public ConfigTree encode(PlatformId value) {
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.getChildBuilder(CONFIG_KEY_PLATFORM_NAME).setValue(value.getName());
                result.getChildBuilder(CONFIG_KEY_PLATFORM_VERSION).setValue(value.getVersion());
                return result.create();
            }
        };
    }

    private TargetPlatformProperty() {
        throw new AssertionError();
    }
}
