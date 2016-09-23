package org.netbeans.gradle.project.model;

public interface ModelRetrievedListener<M> {
    public void updateModel(M model, Throwable error);
}
