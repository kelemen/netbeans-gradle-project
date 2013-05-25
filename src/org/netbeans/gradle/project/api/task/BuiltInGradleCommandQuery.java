package org.netbeans.gradle.project.api.task;

import java.util.Set;
import org.netbeans.gradle.project.api.config.ProfileDef;

public interface BuiltInGradleCommandQuery {
    public Set<String> getSupportedCommands();
    public BuiltInGradleCommand tryGetCommand(ProfileDef profileDef, String command);
}
