package org.netbeans.gradle.project.validate;

import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeListener;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.openide.util.ChangeSupport;

public final class BackgroundValidator {
    private static final MonitorableTaskExecutorService VALIDATOR_PROCESSOR
            = NbTaskExecutors.newExecutor("Gradle-NewProject-Validator-Processor", 1);

    private final AtomicReference<GroupValidator> validatorsRef;
    private final ChangeSupport changes;
    private final UpdateTaskExecutor validationSubmitter;
    private final UpdateTaskExecutor validationExecutor;
    private final UpdateTaskExecutor changeNotifier;
    private final AtomicReference<Problem> currentProblemRef;

    public BackgroundValidator() {
        this.changes = new ChangeSupport(this);
        this.validationSubmitter = new SwingUpdateTaskExecutor(true);
        this.validationExecutor = new GenericUpdateTaskExecutor(VALIDATOR_PROCESSOR);
        this.changeNotifier = new SwingUpdateTaskExecutor(true);
        this.validatorsRef = new AtomicReference<>(null);
        this.currentProblemRef = new AtomicReference<>(null);
    }

    public void setValidators(GroupValidator validators) {
        ExceptionHelper.checkNotNullArgument(validators, "validators");

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
            changeNotifier.execute(new Runnable() {
                @Override
                public void run() {
                    changes.fireChange();
                }
            });
        }
    }

    private Validator<Void> tryGetValidatorInput() {
        assert SwingUtilities.isEventDispatchThread();

        GroupValidator currentValidators = validatorsRef.get();
        return GroupValidator != null
                ? currentValidators.getInputCollector().getInput()
                : null;
    }

    private void submitValidation() {
        final Validator<Void> input = tryGetValidatorInput();

        validationExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Problem problem = input != null
                        ? input.validateInput(null)
                        : Problem.severe("");
                setCurrentProblem(problem);
            }
        });
    }

    public void performValidation() {
        validationSubmitter.execute(new Runnable() {
            @Override
            public void run() {
                submitValidation();
            }
        });
    }

    public Problem getCurrentProblem() {
        return currentProblemRef.get();
    }

    public boolean isValid() {
        Problem currentProblem = getCurrentProblem();
        return currentProblem == null || currentProblem.getLevel() != Problem.Level.SEVERE;
    }
}
