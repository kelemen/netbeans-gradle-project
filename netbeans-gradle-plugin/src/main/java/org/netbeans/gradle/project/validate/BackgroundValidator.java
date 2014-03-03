package org.netbeans.gradle.project.validate;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import javax.swing.event.ChangeEvent;
import javax.swing.event.ChangeListener;
import org.jtrim.collections.Equality;
import org.jtrim.collections.RefLinkedList;
import org.jtrim.collections.RefList;
import org.jtrim.concurrent.GenericUpdateTaskExecutor;
import org.jtrim.concurrent.MonitorableTaskExecutorService;
import org.jtrim.concurrent.TaskExecutor;
import org.jtrim.concurrent.UpdateTaskExecutor;
import org.jtrim.event.EventDispatcher;
import org.jtrim.event.ListenerRef;
import org.jtrim.event.ListenerRegistries;
import org.jtrim.property.MutableProperty;
import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.jtrim.property.ValueConverter;
import org.jtrim.property.swing.SwingProperties;
import org.jtrim.property.swing.SwingPropertySource;
import org.jtrim.swing.concurrent.SwingTaskExecutor;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbTaskExecutors;

import static org.jtrim.property.PropertyFactory.*;

public final class BackgroundValidator {
    private static final MonitorableTaskExecutorService VALIDATOR_PROCESSOR
            = NbTaskExecutors.newExecutor("Gradle-NewProject-Validator-Processor", 1);

    private final UpdateTaskExecutor validationExecutor;

    private final MutableProperty<Problem> currentProblem;
    private final SwingPropertySource<Problem, ChangeListener> currentProblemForSwing;
    private final PropertySource<Boolean> valid;

    private final MostSevereValidator validator;

    public BackgroundValidator() {
        this.validationExecutor = new GenericUpdateTaskExecutor(VALIDATOR_PROCESSOR);
        this.validator = new MostSevereValidator();

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

    public <InputType> ListenerRef addValidator(
            Validator<InputType> validator,
            PropertySource<? extends InputType> input) {

        return addValidator(validator, input, SwingTaskExecutor.getStrictExecutor(true));
    }

    public <InputType> ListenerRef addValidator(
            final Validator<InputType> validator,
            final PropertySource<? extends InputType> input,
            final TaskExecutor inputReaderExecutor) {

        ExceptionHelper.checkNotNullArgument(validator, "validator");
        ExceptionHelper.checkNotNullArgument(input, "input");
        ExceptionHelper.checkNotNullArgument(inputReaderExecutor, "inputReaderExecutor");

        final UpdateTaskExecutor updateReader = new GenericUpdateTaskExecutor(inputReaderExecutor);

        final AtomicReference<InputType> valueRef = new AtomicReference<>(input.getValue());
        final Runnable updateValueTask = new Runnable() {
            @Override
            public void run() {
                valueRef.set(input.getValue());
                performValidation();
            }
        };

        ListenerRef ref1 = input.addChangeListener(new Runnable() {
            @Override
            public void run() {
                updateReader.execute(updateValueTask);
            }
        });
        ListenerRef ref2 = this.validator.addValidator(new Validator<Void>() {
            @Override
            public Problem validateInput(Void inputType) {
                return validator.validateInput(valueRef.get());
            }
        });

        performValidation();

        final ListenerRef result = ListenerRegistries.combineListenerRefs(ref1, ref2);
        return new ListenerRef() {
            @Override
            public boolean isRegistered() {
                return result.isRegistered();
            }

            @Override
            public void unregister() {
                result.unregister();
                performValidation();
            }
        };
    }

    public PropertySource<Problem> currentProblem() {
        return currentProblem;
    }

    public SwingPropertySource<Problem, ChangeListener> currentProblemForSwing() {
        return currentProblemForSwing;
    }

    private void performValidation() {
        validationExecutor.execute(new Runnable() {
            @Override
            public void run() {
                Problem problem = validator.validateInput(null);
                currentProblem.setValue(problem);
            }
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

            return new ListenerRef() {
                @Override
                public boolean isRegistered() {
                    validatorsLock.lock();
                    try {
                        return !elementRef.isRemoved();
                    } finally {
                        validatorsLock.unlock();
                    }
                }

                @Override
                public void unregister() {
                    validatorsLock.lock();
                    try {
                        elementRef.remove();
                    } finally {
                        validatorsLock.unlock();
                    }
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
