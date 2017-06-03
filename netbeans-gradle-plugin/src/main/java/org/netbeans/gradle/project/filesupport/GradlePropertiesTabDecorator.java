package org.netbeans.gradle.project.filesupport;

import java.awt.Component;
import org.netbeans.core.multitabs.TabDecorator;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.util.ContextUtils;
import org.netbeans.gradle.project.util.GradleFileUtils;
import org.netbeans.swing.tabcontrol.TabData;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.windows.TopComponent;

@ServiceProvider(service = TabDecorator.class)
public class GradlePropertiesTabDecorator extends TabDecorator {
    public GradlePropertiesTabDecorator() {
    }

    private static FileObject tryGetFile(TabData tab) {
        Component component = tab.getComponent();
        if (component instanceof TopComponent) {
            TopComponent topComponent = (TopComponent)component;
            return ContextUtils.tryGetFileObjFromContextPreferData(topComponent.getLookup());
        }
        return null;
    }

    private boolean shouldAnnotate(FileObject file) {
        if (!CommonScripts.GRADLE_PROPERTIES_NAME.equalsIgnoreCase(file.getNameExt())) {
            return false;
        }

        FileObject gradleHome = GradleFileUtils.getGradleUserHomeFileObject();
        return !FileUtil.isParentOf(gradleHome, file);
    }

    private String annotateWithFolder(FileObject file, String name) {
        if (shouldAnnotate(file)) {
            FileObject parent = file.getParent();
            if (parent != null) {
                String folderName = parent.getNameExt();
                return name + " [" + folderName + "]";
            }
        }

        return name;
    }


    @Override
    public String getText(TabData tab) {
        FileObject editorFile = tryGetFile(tab);
        if (editorFile != null) {
            return annotateWithFolder(editorFile, tab.getText());
        }
        else {
            return null;
        }
    }
}
