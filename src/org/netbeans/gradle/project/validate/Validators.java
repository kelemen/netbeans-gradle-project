
package org.netbeans.gradle.project.validate;

import java.awt.Color;
import java.util.regex.Pattern;
import javax.swing.JLabel;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import javax.swing.text.JTextComponent;
import org.netbeans.gradle.project.NbStrings;

public final class Validators {
    private static final Pattern LEGAL_FILENAME_PATTERN = Pattern.compile("[^/./\\:*?\"<>|]*");

    public static InputCollector<String> createCollector(
            final JTextComponent component) {
        if (component == null) throw new NullPointerException("component");

        return new InputCollector<String>() {
            @Override
            public String getInput() {
                String result = component.getText();
                return result != null ? result.trim() : "";
            }
        };
    }

    public static Validator<String> createNonEmptyValidator(
            final Problem.Level severity,
            final String errorMessage) {
        if (severity == null) throw new NullPointerException("severity");
        if (errorMessage == null) throw new NullPointerException("errorMessage");

        return new Validator<String>() {
            @Override
            public Problem validateInput(String inputType) {
                return inputType.isEmpty()
                        ? new Problem(severity, errorMessage)
                        : null;
            }
        };
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
        if (pattern == null) throw new NullPointerException("pattern");
        if (severity == null) throw new NullPointerException("severity");
        if (errorMessage == null) throw new NullPointerException("errorMessage");

        return new Validator<String>() {
            @Override
            public Problem validateInput(String inputType) {
                if (!pattern.matcher(inputType).matches()) {
                    return new Problem(severity, errorMessage);
                }
                return null;
            }
        };
    }

    public static <InputType> Validator<InputType> merge(
            final Validator<? super InputType> validator1,
            final Validator<? super InputType> validator2) {

        return new Validator<InputType>() {
            @Override
            public Problem validateInput(InputType inputType) {
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
            }
        };
    }

    public static void connectLabelToProblems(
            final BackgroundValidator validator,
            final JLabel jLabel) {
        if (validator == null) throw new NullPointerException("validator");
        if (jLabel == null) throw new NullPointerException("jLabel");

        jLabel.setText("");
        validator.addChangeListener(new ChangeListener() {
            @Override
            public void stateChanged(ChangeEvent e) {
                Problem currentProblem = validator.getCurrentProblem();
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
            }
        });
    }
}
