package org.netbeans.gradle.project.model;

public interface ModelRetrievedListener {
    public void onComplete(NbGradleModel model, Throwable error);
}
