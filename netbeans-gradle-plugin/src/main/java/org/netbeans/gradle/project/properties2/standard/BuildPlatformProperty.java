
package org.netbeans.gradle.project.properties2.standard;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.gradle.project.api.entry.GradleProjectPlatformQuery;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties2.ConfigPath;
import org.netbeans.gradle.project.properties2.ConfigTree;
import org.netbeans.gradle.project.properties2.PlatformId;
import org.netbeans.gradle.project.properties2.ProjectProfileSettings;
import org.netbeans.gradle.project.properties2.PropertyDef;
import org.netbeans.gradle.project.properties2.PropertyKeyEncodingDef;
import org.netbeans.gradle.project.properties2.PropertyValueDef;
import org.openide.util.Lookup;


public final class BuildPlatformProperty {
    private static final PropertyDef<PlatformId, ProjectPlatform> PROPERTY_DEF = createPropertyDef();
    private static final String CONFIG_KEY_SCRIPT_PLATFORM = "script-platform";
    private static final String CONFIG_KEY_SPEC_NAME = "spec-name";
    private static final String CONFIG_KEY_SPEC_VERSION = "spec-version";

    public static PropertySource<ProjectPlatform> getProperty(ProjectProfileSettings settings) {
        List<ConfigPath> paths = Arrays.asList(ConfigPath.fromKeys(CONFIG_KEY_SCRIPT_PLATFORM));
        return settings.getProperty(paths, getPropertyDef());
    }

    public static PropertyDef<PlatformId, ProjectPlatform> getPropertyDef() {
        return PROPERTY_DEF;
    }

    private static Collection<GradleProjectPlatformQuery> findOwnerQueries(String name) {
        GradleProjectPlatformQuery query = findOwnerQuery(name);
        if (query != null) {
            return null;
        }

        return new ArrayList<>(Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class));
    }

    private static GradleProjectPlatformQuery findOwnerQuery(String name) {
        for (GradleProjectPlatformQuery query: Lookup.getDefault().lookupAll(GradleProjectPlatformQuery.class)) {
            if (query.isOwnerQuery(name)) {
                return query;
            }
        }
        return null;
    }

    public static ProjectPlatform tryGetValue(PlatformId platformId, Collection<GradleProjectPlatformQuery> queries) {
        for (GradleProjectPlatformQuery query: queries) {
            ProjectPlatform platform = query.tryFindPlatformByName(platformId.getName(), platformId.getVersion());
            return platform;
        }
        return null;
    }

    private static PropertySource<ProjectPlatform> projectPlatform(final PlatformId valueKey) {
        if (valueKey == null) {
            return PropertyFactory.constSource(null);
        }

        final Collection<GradleProjectPlatformQuery> queries = findOwnerQueries(valueKey.getName());
        return new PropertySource<ProjectPlatform>() {
            @Override
            public ProjectPlatform getValue() {
                return tryGetValue(valueKey, queries);
            }

            @Override
            public ListenerRef addChangeListener(Runnable listener) {
                List<ListenerRef> refs = new ArrayList<>(queries.size());
                for (GradleProjectPlatformQuery query: queries) {
                    refs.add(query.addPlatformChangeListener(listener));
                }
                return ListenerRegistries.combineListenerRefs(refs);
            }
        };
    }

    private static PropertyValueDef<PlatformId, ProjectPlatform> getValueDef() {
        return new PropertyValueDef<PlatformId, ProjectPlatform>() {
            @Override
            public PropertySource<ProjectPlatform> property(PlatformId valueKey) {
                return projectPlatform(valueKey);
            }

            @Override
            public PlatformId getKeyFromValue(ProjectPlatform value) {
                return value != null
                        ? new PlatformId(value.getName(), value.getVersion())
                        : null;
            }
        };
    }

    private static PropertyDef<PlatformId, ProjectPlatform> createPropertyDef() {
        PropertyDef.Builder<PlatformId, ProjectPlatform> result = new PropertyDef.Builder<>();
        result.setKeyEncodingDef(getEncodingDef());
        result.setValueDef(getValueDef());
        return result.create();
    }

    private static PropertyKeyEncodingDef<PlatformId> getEncodingDef() {
        return new PropertyKeyEncodingDef<PlatformId>() {
            @Override
            public PlatformId decode(ConfigTree config) {
                String version = config.getChildTree(CONFIG_KEY_SPEC_NAME).getValue(null);
                if (version == null) {
                    return null;
                }

                String name = config.getChildTree(CONFIG_KEY_SPEC_NAME).getValue(PlatformId.DEFAULT_NAME);
                return new PlatformId(name, version);
            }

            @Override
            public ConfigTree encode(PlatformId value) {
                ConfigTree.Builder result = new ConfigTree.Builder();
                result.getChildBuilder(CONFIG_KEY_SPEC_NAME).setValue(value.getName());
                result.getChildBuilder(CONFIG_KEY_SPEC_VERSION).setValue(value.getVersion());
                return result.create();
            }
        };
    }

    private BuildPlatformProperty() {
        throw new AssertionError();
    }
}
