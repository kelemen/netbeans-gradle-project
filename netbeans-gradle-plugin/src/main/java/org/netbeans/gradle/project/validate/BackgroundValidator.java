package org.netbeans.gradle.project.validate;

import java.util.concurrent.atomic.AtomicReference;
import javax.swing.SwingUtilities;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.collections.Equality;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.EventDispatcher;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.swing.concurrent.SwingUpdateTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbTaskExecutors;

import static org.jtrim.property.PropertyFactory.*;

public final class BackgroundValidator {
    private static final MonitorableTaskExecutorService VALIDATOR_PROCESSOR
            = NbTaskExecutors.newExecutor("Gradle-NewProject-Validator-Processor", 1);

    private final AtomicReference<GroupValidator> validatorsRef;
    private final UpdateTaskExecutor validationSubmitter;
    private final UpdateTaskExecutor validationExecutor;

    private final MutableProperty<Problem> currentProblem;
    private final SwingPropertySource<Problem, ChangeListener> currentProblemForSwing;
    private final PropertySource<Boolean> valid;

    public BackgroundValidator() {
        this.validationSubmitter = new SwingUpdateTaskExecutor(true);
        this.validationExecutor = new GenericUpdateTaskExecutor(VALIDATOR_PROCESSOR);
        this.validatorsRef = new AtomicReference<>(null);
        this.currentProblem = lazilySetProperty(memProperty((Problem)null, true), Equality.<Problem>referenceEquality());
        this.currentProblemForSwing = SwingProperties.toSwingSource(currentProblem, new EventDispatcher<ChangeListener, Void>() {
            @Override
            public void onEvent(ChangeListener eventListener, Void arg) {
                eventListener.stateChanged(new ChangeEvent(BackgroundValidator.this));
            }
        });
        this.valid = PropertyFactory.convert(currentProblem, new ValueConverter<Problem, Boolean>() {
            @Override
            public Boolean convert(Problem input) {
                return input == null || input.getLevel() != Problem.Level.SEVERE;
            }
        });
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

    public PropertySource<Problem> currentProblem() {
        return currentProblem;
    }

    public SwingPropertySource<Problem, ChangeListener> currentProblemForSwing() {
        return currentProblemForSwing;
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
                currentProblem.setValue(problem);
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

    public PropertySource<Boolean> valid() {
        return valid;
    }

    public boolean isValid() {
        return valid.getValue();
    }
}
