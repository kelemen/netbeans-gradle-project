package org.netbeans.gradle.project.filesupport;

import java.awt.Component;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.event.ChangeListener;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.utils.LazyValues;
import org.openide.WizardDescriptor;
import org.openide.loaders.TemplateWizard;
import org.openide.util.HelpCtx;

final class GradleTemplateWizardPanelWrapper implements WizardDescriptor.Panel<WizardDescriptor> {
    private final MutableProperty<GradleTemplateWizardConfig> config;

    private final Supplier<GradleTemplateWizardPanel> panelRef;

    public GradleTemplateWizardPanelWrapper(MutableProperty<GradleTemplateWizardConfig> config, TemplateWizard wizard) {
        this.config = Objects.requireNonNull(config, "config");

        Objects.requireNonNull(wizard, "wizard");
        this.panelRef = LazyValues.lazyValue(() -> new GradleTemplateWizardPanel(wizard));
    }

    @Override
    public Component getComponent() {
        return panelRef.get();
    }

    @Override
    public HelpCtx getHelp() {
        return null;
    }

    @Override
    public void readSettings(WizardDescriptor settings) {
        panelRef.get().startValidation();
    }

    @Override
    public void storeSettings(WizardDescriptor settings) {
        config.setValue(panelRef.get().getConfig());
    }

    @Override
    public boolean isValid() {
        return panelRef.get().containsValidData();
    }

    @Override
    public void addChangeListener(ChangeListener listener) {
        panelRef.get().addChangeListener(listener);
    }

    @Override
    public void removeChangeListener(ChangeListener listener) {
        panelRef.get().removeChangeListener(listener);
    }
}
