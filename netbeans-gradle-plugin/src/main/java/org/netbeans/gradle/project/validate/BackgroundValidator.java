package org.netbeans.gradle.project.validate;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim2.collections.Equality;
import org.jtrim2.collections.RefLinkedList;
import org.jtrim2.collections.RefList;
import org.jtrim2.event.ListenerRef;
import org.jtrim2.event.ListenerRefs;
import org.jtrim2.executor.GenericUpdateTaskExecutor;
import org.jtrim2.executor.TaskExecutor;
import org.jtrim2.executor.UpdateTaskExecutor;
import org.jtrim2.property.MutableProperty;
import org.jtrim2.property.PropertySource;
import org.jtrim2.property.swing.SwingProperties;
import org.jtrim2.property.swing.SwingPropertySource;
import org.jtrim2.swing.concurrent.SwingExecutors;
import org.netbeans.gradle.project.util.NbTaskExecutors;

import static org.jtrim2.property.PropertyFactory.*;

public final class BackgroundValidator {
    private final UpdateTaskExecutor validationExecutor;

    private final MutableProperty<Problem> currentProblem;
    private final SwingPropertySource<Problem, ChangeListener> currentProblemForSwing;
    private final PropertySource<Boolean> valid;

    private final MostSevereValidator validator;

    public BackgroundValidator() {
        this.validationExecutor = NbTaskExecutors.newDefaultUpdateExecutor();
        this.validator = new MostSevereValidator();

        this.currentProblem = lazilySetProperty(memProperty((Problem)null, true), Equality.<Problem>referenceEquality());
        this.currentProblemForSwing = SwingProperties.toSwingSource(currentProblem, (ChangeListener eventListener, Void arg) -> {
            eventListener.stateChanged(new ChangeEvent(BackgroundValidator.this));
        });
        this.valid = convert(currentProblem, input -> input == null || input.getLevel() != Problem.Level.SEVERE);
    }

    public <InputType> ListenerRef addValidator(
            Validator<InputType> validator,
            PropertySource<? extends InputType> input) {

        return addValidator(validator, input, SwingExecutors.getStrictExecutor(true));
    }

    public <InputType> ListenerRef addValidator(
            final Validator<InputType> validator,
            final PropertySource<? extends InputType> input,
            final TaskExecutor inputReaderExecutor) {

        Objects.requireNonNull(validator, "validator");
        Objects.requireNonNull(input, "input");
        Objects.requireNonNull(inputReaderExecutor, "inputReaderExecutor");

        final UpdateTaskExecutor updateReader = new GenericUpdateTaskExecutor(inputReaderExecutor);

        AtomicReference<InputType> valueRef = new AtomicReference<>(input.getValue());
        Runnable updateValueTask = () -> {
            valueRef.set(input.getValue());
            performValidation();
        };

        ListenerRef ref1 = input.addChangeListener(() -> {
            updateReader.execute(updateValueTask);
        });
        ListenerRef ref2 = this.validator.addValidator((Void inputType) -> validator.validateInput(valueRef.get()));

        performValidation();

        return ListenerRefs.combineListenerRefs(ref1, ref2, this::performValidation);
    }

    public PropertySource<Problem> currentProblem() {
        return currentProblem;
    }

    public SwingPropertySource<Problem, ChangeListener> currentProblemForSwing() {
        return currentProblemForSwing;
    }

    private void performValidation() {
        validationExecutor.execute(() -> {
            Problem problem = validator.validateInput(null);
            currentProblem.setValue(problem);
        });
    }

    public PropertySource<Boolean> valid() {
        return valid;
    }

    public boolean isValid() {
        return valid.getValue();
    }

    private static class MostSevereValidator implements Validator<Void> {
        private final Lock validatorsLock;
        private final RefList<Validator<Void>> validators;

        public MostSevereValidator() {
            this.validatorsLock = new ReentrantLock();
            this.validators = new RefLinkedList<>();
        }

        public ListenerRef addValidator(Validator<Void> validator) {
            final RefList.ElementRef<Validator<Void>> elementRef;

            validatorsLock.lock();
            try {
                elementRef = validators.addLastGetReference(validator);
            } finally {
                validatorsLock.unlock();
            }

            return () -> {
                validatorsLock.lock();
                try {
                    elementRef.remove();
                } finally {
                    validatorsLock.unlock();
                }
            };
        }

        public List<Validator<Void>> getValidators() {
            validatorsLock.lock();
            try {
                return new ArrayList<>(validators);
            } finally {
                validatorsLock.unlock();
            }
        }

        @Override
        public Problem validateInput(Void inputType) {
            Problem mostSevere = null;
            for (Validator<Void> current: getValidators()) {
                Problem currentProblem = current.validateInput(null);
                if (currentProblem != null) {
                    if (currentProblem.getLevel() == Problem.Level.SEVERE) {
                        return currentProblem;
                    }

                    if (mostSevere == null) {
                        mostSevere = currentProblem;
                    }
                    else if (mostSevere.getLevel().getIntValue() < currentProblem.getLevel().getIntValue()) {
                        mostSevere = currentProblem;
                    }
                }
            }
            return mostSevere;
        }
    }
}
