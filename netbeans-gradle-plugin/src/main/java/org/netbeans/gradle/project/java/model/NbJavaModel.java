package org.netbeans.gradle.project.java.model;

import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.ObjectStreamException;
import java.io.Serializable;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.api.modelquery.GradleTarget;
import org.netbeans.gradle.project.util.GradleVersions;

public final class NbJavaModel implements Serializable {
    private static final long serialVersionUID = 1L;

    private final GradleTarget evaluationEnvironment;
    private final JavaModelSource modelSource;
    private final NbJavaModule mainModule;

    private NbJavaModel(
            GradleTarget evaluationEnvironment,
            JavaModelSource modelSource,
            NbJavaModule mainModule) {

        this.evaluationEnvironment = evaluationEnvironment;
        this.modelSource = modelSource;
        this.mainModule = mainModule;
    }

    public static NbJavaModel createModel(
            GradleTarget evaluationEnvironment,
            JavaModelSource modelSource,
            NbJavaModule mainModule) {

        ExceptionHelper.checkNotNullArgument(evaluationEnvironment, "evaluationEnvironment");
        ExceptionHelper.checkNotNullArgument(modelSource, "modelSource");
        ExceptionHelper.checkNotNullArgument(mainModule, "mainModule");

        return new NbJavaModel(evaluationEnvironment, modelSource, mainModule);
    }

    public GradleTarget getEvaluationEnvironment() {
        return evaluationEnvironment;
    }

    public JavaModelSource getModelSource() {
        return modelSource;
    }

    public NbJavaModule getMainModule() {
        return mainModule;
    }

    private Object writeReplace() {
        return new SerializedFormat(this);
    }

    private void readObject(ObjectInputStream stream) throws InvalidObjectException {
        throw new InvalidObjectException("Use proxy.");
    }

    private static final class SerializedFormat implements Serializable {
        private static final long serialVersionUID = 1L;

        private final GradleTarget evaluationEnvironment;
        private final JavaModelSource modelSource;
        private final NbJavaModule mainModule;

        public SerializedFormat(NbJavaModel source) {
            this.evaluationEnvironment = source.evaluationEnvironment;
            this.modelSource = source.modelSource;
            this.mainModule = source.mainModule;
        }

        public GradleTarget getEvaluationEnvironment() {
            return evaluationEnvironment != null ? evaluationEnvironment : GradleVersions.DEFAULT_TARGET;
        }

        private Object readResolve() throws ObjectStreamException {
            return new NbJavaModel(evaluationEnvironment, modelSource, mainModule);
        }
    }
}
