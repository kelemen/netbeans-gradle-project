package org.netbeans.gradle.project.properties.standard;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import org.jtrim2.utils.ExceptionHelper;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.properties.PlatformSelectionMode;
import org.netbeans.gradle.project.properties.PlatformSelector;
import org.netbeans.gradle.project.properties.ScriptPlatform;
import org.netbeans.gradle.project.properties.global.PlatformOrder;
import org.openide.filesystems.FileObject;

public final class ExplicitPlatformRef implements PlatformSelector {
    private final Set<String> installDirs;

    public ExplicitPlatformRef(Collection<String> installDirs) {
        this.installDirs = Collections.unmodifiableSet(new LinkedHashSet<>(installDirs));
        ExceptionHelper.checkNotNullElements(this.installDirs, "installDirs");
    }

    public ExplicitPlatformRef(JavaPlatform platform) {
        this(getInstallDirs(platform));
    }

    private JavaPlatform selectRawPlatform(List<? extends JavaPlatform> platforms, PlatformOrder order) {
        Objects.requireNonNull(order, "order");
        for (JavaPlatform platform: platforms) {
            if (Objects.equals(installDirs, getInstallDirs(platform))) {
                return platform;
            }
        }
        return JavaPlatform.getDefault();
    }

    @Override
    public ScriptPlatform selectPlatform(List<? extends JavaPlatform> platforms, PlatformOrder order) {
        return new ScriptPlatform(selectRawPlatform(platforms, order), PlatformSelectionMode.BY_LOCATION);
    }

    private static Set<String> getInstallDirs(JavaPlatform platform) {
        Collection<FileObject> installFolders = platform.getInstallFolders();
        Set<String> result = new LinkedHashSet<>(installFolders.size());

        for (FileObject installFolder: installFolders) {
            String path = installFolder.getPath();
            result.add(path);
        }

        return result;
    }

    public static ExplicitPlatformRef tryParse(ConfigTree config) {
        List<ConfigTree> installDirs = config.getChildTree("location").getChildTrees("install-dir");
        if (installDirs.isEmpty()) {
            return null;
        }

        List<String> result = new ArrayList<>(installDirs.size());
        for (ConfigTree dir: installDirs) {
            result.add(dir.getValue(""));
        }
        return new ExplicitPlatformRef(result);
    }

    @Override
    public ConfigTree toConfig() {
        ConfigTree.Builder result = new ConfigTree.Builder();
        ConfigTree.Builder location = result.addChildBuilder("location");
        for (String installDir: installDirs) {
            location.addChildBuilder("install-dir").setValue(installDir);
        }
        return result.create();
    }
}
