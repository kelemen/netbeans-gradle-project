package org.netbeans.gradle.project.filesupport;

import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class GradleTemplateConsts {
    public static final String CATEGORY_NAME = "gradle-files";
    public static final String FOLDER_NAME = "GradleTemplate";
    public static final String TEMPLATE_SCRIPT_NAME = "script.gradle";

    public static FileObject getTemplateFolder() {
        return FileUtil.getConfigFile("Templates/GradleTemplate");
    }

    private GradleTemplateConsts() {
        throw new AssertionError();
    }
}
