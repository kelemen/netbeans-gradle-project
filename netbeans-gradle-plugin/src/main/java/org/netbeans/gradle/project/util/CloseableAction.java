package org.netbeans.gradle.project.util;

public interface CloseableAction {
    public static interface Ref extends AutoCloseable {
        @Override
        public void close();
    }

    public static CloseableAction.Ref CLOSED_REF = () -> { };

    public Ref open();
}
