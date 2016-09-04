package org.netbeans.gradle.project.properties;

import java.util.List;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.api.config.ConfigTree;
import org.netbeans.gradle.project.properties.global.PlatformOrder;

public interface PlatformSelector {
    public ScriptPlatform selectPlatform(
            List<? extends JavaPlatform> platforms,
            PlatformOrder order);
    public ConfigTree toConfig();
}
