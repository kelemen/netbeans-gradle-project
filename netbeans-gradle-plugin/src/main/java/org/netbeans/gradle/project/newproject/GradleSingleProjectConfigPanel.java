package org.netbeans.gradle.project.newproject;

import java.awt.Component;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.event.ChangeListener;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public final class GradleSingleProjectConfigPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    private final AtomicReference<GradleSingleProjectPropertiesPanel> panel;
    private final AtomicReference<GradleSingleProjectConfig> configRef;
    private final WizardDescriptor wizard;

    public GradleSingleProjectConfigPanel(AtomicReference<GradleSingleProjectConfig> configRef, WizardDescriptor wizard) {
        if (configRef == null) throw new NullPointerException("configRef");

        this.configRef = configRef;
        this.wizard = wizard;
        this.panel = new AtomicReference<>();
    }

    private GradleSingleProjectPropertiesPanel getPanel() {
        GradleSingleProjectPropertiesPanel result = panel.get();
        if (result == null) {
            panel.compareAndSet(null, new GradleSingleProjectPropertiesPanel(wizard));
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
        getPanel().startValidation();
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
