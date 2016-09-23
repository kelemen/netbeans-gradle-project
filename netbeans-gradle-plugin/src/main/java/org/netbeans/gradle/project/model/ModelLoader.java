package org.netbeans.gradle.project.model;

public interface ModelLoader<M> {
    public void fetchModel(
            boolean mayFetchFromCache,
            ModelRetrievedListener<? super M> listener,
            Runnable aboutToCompleteListener);
}
