package org.netbeans.gradle.project.properties;

import java.io.Serializable;
import java.util.Collection;
import org.netbeans.gradle.project.api.config.ProfileDef;

// !!! Warning: This class cannot be moved or renamed otherwise we will fail
//              to read SavedProfileDef instances already saved.
final class SavedProfileDef implements Serializable {
    private static final long serialVersionUID = 1L;
    private final String groupName;
    private final String fileName;

    public SavedProfileDef(ProfileDef def) {
        this.groupName = def.getGroupName();
        this.fileName = def.getFileName();
    }

    public NbGradleConfiguration findSameConfig(Collection<? extends NbGradleConfiguration> configs) {
        NbGradleConfiguration toSearch = new NbGradleConfiguration(new ProfileDef(groupName, fileName, "?"));
        for (NbGradleConfiguration config: configs) {
            if (toSearch.equals(config)) {
                return config;
            }
        }
        return null;
    }

}
