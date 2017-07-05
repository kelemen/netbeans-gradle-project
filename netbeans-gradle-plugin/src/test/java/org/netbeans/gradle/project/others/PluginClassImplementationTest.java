package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.junit.Test;

import static org.junit.Assert.*;
import static org.mockito.Matchers.*;
import static org.mockito.Mockito.*;

public class PluginClassImplementationTest {
    private static ClassFinder classFinder(final Class<?> type) {
        return ReflectionHelper.constClassFinder(type);
    }

    private static void verifyRunForwarded(Runnable instance, Runnable wrapped) {
        assertNotNull(instance);
        verifyZeroInteractions(wrapped);

        instance.run();

        verify(wrapped).run();
    }

    @Test
    public void testDelegateToInterface() {
        Runnable wrapped = mock(Runnable.class);

        PluginClassImplementation impl
                = new PluginClassImplementation(classFinder(Runnable.class), new MyRunnable(wrapped));

        Runnable instance = (Runnable)impl.tryGetAsPluginClass();

        verifyRunForwarded(instance, wrapped);
    }

    @Test
    public void testDelegateToInterfaceWithException() throws Throwable {
        Runnable2 wrapped = mock(Runnable2.class);
        final InvocationHandler run2Handler = mock(InvocationHandler.class);

        InvocationHandlerFactory exceptionalCase = (Object proxy, Method method, Object[] args) -> {
            return "run2".equals(method.getName()) ? run2Handler : null;
        };

        PluginClassImplementation impl = new PluginClassImplementation(
                classFinder(Runnable2.class),
                new MyRunnable(wrapped),
                exceptionalCase);

        Runnable2 instance = (Runnable2)impl.tryGetAsPluginClass();

        verifyRunForwarded(instance, wrapped);

        verifyZeroInteractions(run2Handler);

        instance.run2();
        verify(wrapped, never()).run2();
        verify(run2Handler).invoke(same(instance), any(Method.class), any(Object[].class));
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

    public interface Runnable2 extends Runnable {
        public void run2();
    }
}
