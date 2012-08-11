package org.netbeans.gradle.project;

import org.netbeans.gradle.project.model.NbGradleModel;

public interface ModelRetrievedListener {
    public void onComplete(NbGradleModel model, Throwable error);
}
