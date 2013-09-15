package org.netbeans.gradle.project.model;

import java.io.File;
import java.util.Map;

public interface GradleBuildInfo {
    public GradleProjectInfo getDefaultProjectInfo();
    public Map<File, GradleProjectInfo> getAllProjectInfos();
    public Object tryGetBuildInfoResult(Object key);
}
