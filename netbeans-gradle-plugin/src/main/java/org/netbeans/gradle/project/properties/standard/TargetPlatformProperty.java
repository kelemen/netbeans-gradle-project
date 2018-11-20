package org.netbeans.gradle.project.properties.standard;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.api.config.ConfigPath;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.config.PropertyDef;
import org.netbeans.gradle.project.api.config.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.api.config.PropertyValueDef;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.query.J2SEPlatformFromScriptQuery;
import org.netbeans.gradle.project.util.CachedLookupValue;
import org.openide.util.Lookup;

public final class TargetPlatformProperty {
    private static final String CONFIG_KEY_PLATFORM_NAME = "target-platform-name";
    private static final String CONFIG_KEY_PLATFORM_VERSION = "target-platform";

    private static final List<ConfigPath> CONFIG_ROOT = Collections.unmodifiableList(Arrays.asList(
            ConfigPath.fromKeys(CONFIG_KEY_PLATFORM_NAME),
            ConfigPath.fromKeys(CONFIG_KEY_PLATFORM_VERSION)));

    public static final PropertyDef<PlatformId, ProjectPlatform> PROPERTY_DEF = createPropertyDef();

    private static PropertyDef<PlatformId, ProjectPlatform> createPropertyDef() {
        PropertyDef.Builder<PlatformId, ProjectPlatform> result
                = new PropertyDef.Builder<>(CONFIG_ROOT);

        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(getValueDef());
        return result.create();
    }

    public static PropertySource<ProjectPlatform> defaultValue(
            final NbGradleProject project) {

        final CachedLookupValue<JavaExtension> javaExtRef
                = new CachedLookupValue<>(project, JavaExtension.class);

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
                Objects.requireNonNull(listener, "listener");

                ListenerRef ref1 = project.currentModel().addChangeListener(listener);
                ListenerRef ref2 = JavaPlatformUtils.installedPlatforms().addChangeListener(listener);

                return ListenerRefs.combineListenerRefs(ref1, ref2);
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
                return value != null ? new PlatformId(value) : null;
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
