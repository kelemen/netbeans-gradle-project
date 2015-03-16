package org.netbeans.gradle.project.properties;

public interface ProfileValuesEditor {
    public void displayValues();
    public void readFromGui();

    public void applyValues();
}
