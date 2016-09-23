package org.netbeans.gradle.project.model;

public interface ModelRetrievedListener<M> {
    public void onComplete(M model, Throwable error);
}
