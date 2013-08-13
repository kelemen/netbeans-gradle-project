package org.netbeans.gradle.project.validate;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.openide.util.ChangeSupport;
import org.openide.util.RequestProcessor;

public final class BackgroundValidator {
    private static final RequestProcessor VALIDATOR_PROCESSOR
            = new RequestProcessor("Gradle-NewProject-Validator-Processor", 1, true);

    private final AtomicReference<GroupValidator> validatorsRef;
    private final ChangeSupport changes;
    private final AtomicBoolean validationSubmitted;
    private final AtomicReference<Problem> currentProblemRef;

    public BackgroundValidator() {
        this.changes = new ChangeSupport(this);
        this.validationSubmitted = new AtomicBoolean(false);
        this.validatorsRef = new AtomicReference<GroupValidator>(null);
        this.currentProblemRef = new AtomicReference<Problem>(null);
    }

    public void setValidators(GroupValidator validators) {
        if (validators == null) throw new NullPointerException("validators");

        if (!validatorsRef.compareAndSet(null, validators)) {
            if (validatorsRef.get() != validators) {
                throw new IllegalStateException("ValidationGroup has already been set with a different value.");
            }
        }
        performValidation();
    }

    public void addChangeListener(ChangeListener listener) {
        changes.addChangeListener(listener);
    }

    public void removeChangeListener(ChangeListener listener) {
        changes.removeChangeListener(listener);
    }

    private void setCurrentProblem(Problem newProblem) {
        Problem prevValue = currentProblemRef.getAndSet(newProblem);

        if (prevValue != newProblem) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    changes.fireChange();
                }
            });
        }
    }

    public void performValidation() {
        if (validationSubmitted.compareAndSet(false, true)) {
            SwingUtilities.invokeLater(new Runnable() {
                @Override
                public void run() {
                    validationSubmitted.set(false);
                    final GroupValidator currentValidators = validatorsRef.get();
                    if (currentValidators == null) {
                        setCurrentProblem(Problem.severe(""));
                        return;
                    }

                    final Validator<Void> input
                            = currentValidators.getInputCollector().getInput();

                    VALIDATOR_PROCESSOR.execute(new Runnable() {
                        @Override
                        public void run() {
                            Problem problem = input.validateInput(null);
                            setCurrentProblem(problem);
                        }
                    });
                }
            });
        }
    }

    public Problem getCurrentProblem() {
        return currentProblemRef.get();
    }

    public boolean isValid() {
        Problem currentProblem = getCurrentProblem();
        return currentProblem == null || currentProblem.getLevel() != Problem.Level.SEVERE;
    }
}
