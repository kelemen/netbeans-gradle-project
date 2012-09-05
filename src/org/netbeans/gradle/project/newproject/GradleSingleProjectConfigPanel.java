package org.netbeans.gradle.project.newproject;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public final class GradleSingleProjectConfigPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    private final AtomicReference<GradleSingleProjectPropertiesPanel> panel;
    private final AtomicReference<GradleSingleProjectConfig> configRef;

    public GradleSingleProjectConfigPanel(AtomicReference<GradleSingleProjectConfig> configRef) {
        if (configRef == null) throw new NullPointerException("configRef");

        this.configRef = configRef;
        this.panel = new AtomicReference<GradleSingleProjectPropertiesPanel>();
    }

    private GradleSingleProjectPropertiesPanel getPanel() {
        GradleSingleProjectPropertiesPanel result = panel.get();
        if (result == null) {
            panel.compareAndSet(null, new GradleSingleProjectPropertiesPanel());
            result = panel.get();
        }
        return result;
    }

    @Override
    public Component getComponent() {
        return getPanel();
    }

    @Override
    public HelpCtx getHelp() {
        return null;
    }

    @Override
    public void readSettings(WizardDescriptor settings) {
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        configRef.set(getPanel().getConfig());
    }

    @Override
    public boolean isValid() {
        return getPanel().containsValidData();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        getPanel().addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        getPanel().removeChangeListener(listener);
    }
}
