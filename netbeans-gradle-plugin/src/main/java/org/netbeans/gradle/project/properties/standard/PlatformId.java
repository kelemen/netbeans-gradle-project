package org.netbeans.gradle.project.properties.standard;

import java.util.List;
import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.api.java.platform.Specification;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.api.entry.ProjectPlatform;
import org.netbeans.gradle.project.properties.PlatformSelectionMode;
import org.netbeans.gradle.project.properties.PlatformSelector;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.openide.modules.SpecificationVersion;

public final class PlatformId implements PlatformSelector {
    public static final String DEFAULT_NAME = "j2se";

    private static final String GENERIC_PLATFORM_NAME_NODE = "spec-name";
    private static final String GENERIC_PLATFORM_VERSION_NODE = "spec-version";

    private final String name;
    private final String version;

    public PlatformId(ProjectPlatform platform) {
        this(platform.getName(), platform.getVersion());
    }

    public PlatformId(String name, String version) {
        ExceptionHelper.checkNotNullArgument(name, "name");
        ExceptionHelper.checkNotNullArgument(version, "version");

        this.name = name;
        this.version = version;
    }

    public static PlatformId tryGetIdOfPlatform(JavaPlatform platform) {
        Specification spec = platform.getSpecification();
        if (spec == null) {
            return null;
        }

        SpecificationVersion version = spec.getVersion();
        if (version == null) {
            return null;
        }

        String name = spec.getName();
        String versionStr = version.toString();
        return new PlatformId(name, versionStr);
    }

    public static String getDisplayNameOfPlatform(ProjectPlatform platform) {
        StringBuilder result = new StringBuilder();
        result.append(platform.getDisplayName());
        result.append(" (");
        result.append(platform.getName());
        result.append(" / ");
        result.append(platform.getVersion());
        result.append(")");

        return result.toString();
    }

    public static String getDisplayNameOfPlatform(JavaPlatform platform) {
        StringBuilder result = new StringBuilder();
        result.append(platform.getDisplayName());
        result.append(" (");

        PlatformId id = tryGetIdOfPlatform(platform);
        if (id == null) {
            result.append("unknown version");
        }
        else {
            result.append(id.getName());
            result.append(" / ");
            result.append(id.getVersion());
        }

        result.append(")");

        return result.toString();
    }

    public String getName() {
        return name;
    }

    public String getVersion() {
        return version;
    }

    public static PlatformId tryDecode(ConfigTree config) {
        ConfigTree version = config.getChildTree(GENERIC_PLATFORM_VERSION_NODE);
        String versionStr = version.getValue(null);
        if (versionStr == null) {
            return null;
        }

        ConfigTree name = config.getChildTree(GENERIC_PLATFORM_NAME_NODE);
        return new PlatformId(name.getValue(PlatformId.DEFAULT_NAME), versionStr);
    }

    private JavaPlatform selectRawPlatform(List<? extends JavaPlatform> platforms, PlatformOrder order) {
        List<JavaPlatform> orderedPlatforms = order.orderPlatforms(platforms);
        return JavaPlatformUtils.tryChooseFromOrderedPlatforms(name, version, orderedPlatforms);
    }

    @Override
    public ScriptPlatform selectPlatform(List<? extends JavaPlatform> platforms, PlatformOrder order) {
        return new ScriptPlatform(selectRawPlatform(platforms, order), PlatformSelectionMode.BY_VERSION);
    }

    @Override
    public ConfigTree toConfig() {
        ConfigTree.Builder result = new ConfigTree.Builder();
        result.getChildBuilder(GENERIC_PLATFORM_NAME_NODE).setValue(getName());
        result.getChildBuilder(GENERIC_PLATFORM_VERSION_NODE).setValue(getVersion());
        return result.create();
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 29 * hash + Objects.hashCode(this.name);
        hash = 29 * hash + Objects.hashCode(this.version);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (getClass() != obj.getClass()) return false;

        final PlatformId other = (PlatformId)obj;
        return Objects.equals(this.name, other.name)
                && Objects.equals(this.version, other.version);
    }

    @Override
    public String toString() {
        return "PlatformId{" + "name=" + name + ", version=" + version + '}';
    }
}
