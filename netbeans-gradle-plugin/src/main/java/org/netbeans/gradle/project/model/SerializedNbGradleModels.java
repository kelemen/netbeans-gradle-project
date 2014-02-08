package org.netbeans.gradle.project.model;

import java.io.Serializable;
import org.netbeans.gradle.project.NbGradleProject;

public final class SerializedNbGradleModels implements Serializable {
    private static final long serialVersionUID = 1L;

    public SerializedNbGradleModels(NbGradleModel model) {
    }

    public NbGradleModel deserializeModel(NbGradleProject ownerProject) {
        throw new UnsupportedOperationException("Not implemeted yet");
    }
}
