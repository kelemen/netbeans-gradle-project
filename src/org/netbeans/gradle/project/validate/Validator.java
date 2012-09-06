package org.netbeans.gradle.project.validate;

public interface Validator<InputType> {
    public Problem validateInput(InputType inputType);
}
