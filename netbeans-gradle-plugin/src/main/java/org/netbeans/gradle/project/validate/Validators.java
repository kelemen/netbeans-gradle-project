
package org.netbeans.gradle.project.validate;

import java.awt.Color;
import java.util.Objects;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.text.JTextComponent;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertyFactory;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingProperties;
import org.netbeans.gradle.project.NbStrings;
import org.openide.WizardDescriptor;

public final class Validators {
    private static final Pattern LEGAL_FILENAME_PATTERN = Pattern.compile("[^/./\\:*?\"<>|]*");
    private static final Pattern LEGAL_FILENAME_PATTERN_WITH_EXTENSION= Pattern.compile("[^/\\:*?\"<>|]*");

    public static PropertySource<String> trimmedText(JTextComponent component) {
        MutableProperty<String> property = SwingProperties.textProperty(component);
        return PropertyFactory.convert(property, input -> input != null ? input.trim() : "");
    }

    public static Validator<String> createNonEmptyValidator(
            final Problem.Level severity,
            final String errorMessage) {
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(errorMessage, "errorMessage");

        return (String inputType) -> {
            return inputType.isEmpty()
                    ? new Problem(severity, errorMessage)
                    : null;
        };
    }

    public static Validator<String> createFileNameWithExtensionValidator(
            Problem.Level severity,
            String errorMessage) {
        return createPatternValidator(LEGAL_FILENAME_PATTERN_WITH_EXTENSION, severity, errorMessage);
    }

    public static Validator<String> createFileNameValidator(
            Problem.Level severity,
            String errorMessage) {
        return createPatternValidator(LEGAL_FILENAME_PATTERN, severity, errorMessage);
    }

    public static Validator<String> createPatternValidator(
            final Pattern pattern,
            final Problem.Level severity,
            final String errorMessage) {
        Objects.requireNonNull(pattern, "pattern");
        Objects.requireNonNull(severity, "severity");
        Objects.requireNonNull(errorMessage, "errorMessage");

        return (String inputType) -> {
            if (!pattern.matcher(inputType).matches()) {
                return new Problem(severity, errorMessage);
            }
            return null;
        };
    }

    public static <InputType> Validator<InputType> merge(
            final Validator<? super InputType> validator1,
            final Validator<? super InputType> validator2) {

        return (InputType inputType) -> {
            Problem problem1 = validator1.validateInput(inputType);
            Problem problem2 = validator2.validateInput(inputType);

            if (problem1 == null) {
                return problem2;
            }
            if (problem2 == null) {
                return problem1;
            }

            return problem1.getLevel().getIntValue() >= problem2.getLevel().getIntValue()
                    ? problem1
                    : problem2;
        };
    }

    public static ListenerRef connectLabelToProblems(
            BackgroundValidator validator,
            final JLabel jLabel) {
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(jLabel, "jLabel");

        jLabel.setText("");

        final PropertySource<Problem> validatorProblem = validator.currentProblem();
        return validatorProblem.addChangeListener(() -> {
            Problem currentProblem = validatorProblem.getValue();
            String message = currentProblem != null
                    ? currentProblem.getMessage()
                    : "";
            if (message.isEmpty()) {
                jLabel.setText("");
            }
            else {
                assert currentProblem != null;
                String title;
                Color labelColor;
                switch (currentProblem.getLevel()) {
                    case INFO:
                        labelColor = Color.BLACK;
                        title = NbStrings.getInfoCaption();
                        break;
                    case WARNING:
                        labelColor = Color.ORANGE.darker();
                        title = NbStrings.getWarningCaption();
                        break;
                    case SEVERE:
                        labelColor = Color.RED;
                        title = NbStrings.getErrorCaption();
                        break;
                    default:
                        throw new AssertionError(currentProblem.getLevel().name());
                }

                jLabel.setForeground(labelColor);
                jLabel.setText(title + ": " + message);
            }
        });
    }

    public static ListenerRef connectWizardDescriptorToProblems(
            BackgroundValidator validator,
            final WizardDescriptor wizard) {
        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(wizard, "wizard");

        wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);

        final PropertySource<Problem> validatorProblem = validator.currentProblem();
        return validatorProblem.addChangeListener(() -> {
            Problem currentProblem = validatorProblem.getValue();
            String message = currentProblem != null
                    ? currentProblem.getMessage()
                    : "";
            if (message.isEmpty()) {
                wizard.putProperty(WizardDescriptor.PROP_ERROR_MESSAGE, null);
            }
            else {
                assert currentProblem != null;
                String level;
                switch (currentProblem.getLevel()) {
                    case INFO:
                        level = WizardDescriptor.PROP_INFO_MESSAGE;
                        break;
                    case WARNING:
                        level = WizardDescriptor.PROP_WARNING_MESSAGE;
                        break;
                    case SEVERE:
                        level = WizardDescriptor.PROP_ERROR_MESSAGE;
                        break;
                    default:
                        throw new AssertionError(currentProblem.getLevel().name());
                }
                wizard.putProperty(level, message);
            }
        });

    }
}
