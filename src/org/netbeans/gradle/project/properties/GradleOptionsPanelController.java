package org.netbeans.gradle.project.properties;

import java.beans.PropertyChangeListener;
import java.io.File;
import javax.swing.JComponent;
import org.netbeans.spi.options.OptionsPanelController;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.HelpCtx;
import org.openide.util.Lookup;

@OptionsPanelController.SubRegistration(
        location = "Advanced",
        displayName = "#AdvancedOption_DisplayName_Gradle",
        keywords = "#AdvancedOption_Keywords_Gradle",
        keywordsCategory = "Advanced/Gradle")
public final class GradleOptionsPanelController extends OptionsPanelController {
    private GradleSettingsPanel panel;

    private GradleSettingsPanel getPanel() {
        if (panel == null) {
            panel = new GradleSettingsPanel();
        }
        return panel;
    }

    @Override
    public void update() {
        getPanel().updateSettings();
    }

    private static FileObject strToFileObject(String strPath) {
        if (strPath.isEmpty()) {
            return null;
        }

        File file = new File(strPath);
        file = FileUtil.normalizeFile(file);
        return FileUtil.toFileObject(file);
    }

    @Override
    public void applyChanges() {
        FileObject gradleHomeObj = strToFileObject(getPanel().getGradleHome());
        GlobalGradleSettings.getGradleHome().setValue(gradleHomeObj);
    }

    @Override
    public void cancel() {
    }

    @Override
    public boolean isValid() {
        return true;
    }

    @Override
    public boolean isChanged() {
        return true;
    }

    @Override
    public HelpCtx getHelpCtx() {
        return null;
    }

    @Override
    public JComponent getComponent(Lookup masterLookup) {
        return getPanel();
    }

    @Override
    public void addPropertyChangeListener(PropertyChangeListener l) {
    }

    @Override
    public void removePropertyChangeListener(PropertyChangeListener l) {
    }
}
