package org.netbeans.gradle.project.validate;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public final class GroupValidator {
    private final Lock mainLock;
    private final List<InputAndValidator<?>> validators;
    private final InputCollector<Validator<Void>> inputCollector;

    public GroupValidator() {
        this.mainLock = new ReentrantLock();
        this.validators = new LinkedList<InputAndValidator<?>>();

        this.inputCollector = new InputCollector<Validator<Void>>() {
            @Override
            public Validator<Void> getInput() {
                List<InputAndValidator<?>> currentValidators;
                mainLock.lock();
                try {
                    currentValidators = new ArrayList<InputAndValidator<?>>(validators);
                } finally {
                    mainLock.unlock();
                }

                List<Validator<Void>> fetchedValidators
                        = new ArrayList<Validator<Void>>(currentValidators.size());
                for (InputAndValidator<?> element: currentValidators) {
                    fetchedValidators.add(element.getValidatorWithInput());
                }

                return new MostSevereValidator(fetchedValidators);
            }
        };
    }

    public <InputType> void addValidator(
            Validator<InputType> validator,
            InputCollector<? extends InputType> inputCollector) {

        if (validator == null) throw new NullPointerException("validator");
        if (inputCollector == null) throw new NullPointerException("inputCollector");

        InputAndValidator<InputType> toAdd = new InputAndValidator<InputType>(validator, inputCollector);
        mainLock.lock();
        try {
            validators.add(toAdd);
        } finally {
            mainLock.unlock();
        }
    }

    public InputCollector<Validator<Void>> getInputCollector() {
        return inputCollector;
    }

    private static class MostSevereValidator implements Validator<Void> {
        private final List<Validator<Void>> validators;

        public MostSevereValidator(List<Validator<Void>> validators) {
            this.validators = validators;
        }

        @Override
        public Problem validateInput(Void inputType) {
            Problem mostSevere = null;
            for (Validator<Void> current: validators) {
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

    private static class InputAndValidator<InputType> {
        private final Validator<InputType> validator;
        private final InputCollector<? extends InputType> collector;

        public InputAndValidator(
                Validator<InputType> validator,
                InputCollector<? extends InputType> collector) {
            if (validator == null) throw new NullPointerException("validator");
            if (collector == null) throw new NullPointerException("collector");

            this.validator = validator;
            this.collector = collector;
        }

        public Validator<Void> getValidatorWithInput() {
            return new FetchedValidator<InputType>(collector.getInput(), validator);
        }
    }

    private static class FetchedValidator<InputType> implements Validator<Void> {
        private final InputType input;
        private final Validator<InputType> validator;

        public FetchedValidator(InputType input, Validator<InputType> validator) {
            this.input = input;
            this.validator = validator;
        }

        @Override
        public Problem validateInput(Void inputType) {
            return validator.validateInput(input);
        }
    }
}
