package org.netbeans.gradle.project.properties;

import java.io.IOException;
import java.nio.file.Path;
import org.netbeans.gradle.project.api.config.ProfileKey;

public interface ProfileLocationProvider {
    public ProfileKey getKey();
    public Path tryGetOutputPath() throws IOException;
    public ProfileFileDef tryGetOutputDef() throws IOException;
}
