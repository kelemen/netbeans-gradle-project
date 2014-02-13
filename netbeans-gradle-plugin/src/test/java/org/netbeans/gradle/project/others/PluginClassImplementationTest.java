package org.netbeans.gradle.project.others;

import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

public class PluginClassImplementationTest {
    private static ClassFinder classFinder(final Class<?> type) {
        return new ClassFinder() {
            @Override
            public Class<?> tryGetClass() {
                return type;
            }
        };
    }

    @Test
    public void testTryGetAsPluginClass() {
        Runnable wrapped = mock(Runnable.class);

        PluginClassImplementation impl
                = new PluginClassImplementation(classFinder(Runnable.class), new MyRunnable(wrapped));

        Runnable instance = (Runnable)impl.tryGetAsPluginClass();
        assertNotNull(instance);
        verifyZeroInteractions(wrapped);

        instance.run();

        verify(wrapped).run();
    }

    // Do not implement Runnable, must be a public class
    public static final class MyRunnable {
        private final Runnable wrapped;

        public MyRunnable(Runnable wrapped) {
            this.wrapped = wrapped;
        }

        public void run() {
            wrapped.run();
        }
    }
}
