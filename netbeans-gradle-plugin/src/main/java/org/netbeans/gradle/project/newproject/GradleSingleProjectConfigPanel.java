package org.netbeans.gradle.project.newproject;

import java.awt.Component;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.event.ChangeListener;
import org.jtrim2.utils.LazyValues;
import org.openide.WizardDescriptor;
import org.openide.util.HelpCtx;

public final class GradleSingleProjectConfigPanel implements WizardDescriptor.Panel<WizardDescriptor> {
    private final Supplier<GradleSingleProjectPropertiesPanel> panelRef;
    private final Consumer<? super GradleSingleProjectConfig> configRef;

    public GradleSingleProjectConfigPanel(
            WizardDescriptor wizard,
            Consumer<? super GradleSingleProjectConfig> configRef) {
        this.configRef = Objects.requireNonNull(configRef, "configRef");

        Objects.requireNonNull(wizard, "wizard");
        this.panelRef = LazyValues.lazyValue(() -> new GradleSingleProjectPropertiesPanel(wizard));
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
        configRef.accept(panelRef.get().getConfig());
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
