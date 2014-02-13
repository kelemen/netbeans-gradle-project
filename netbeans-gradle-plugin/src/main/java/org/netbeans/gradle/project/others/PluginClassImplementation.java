package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.concurrent.atomic.AtomicReference;

public final class PluginClassImplementation {
    private final ClassFinder type;
    private final Object delegateInstance;
    private final AtomicReference<Object> instanceRef;

    public PluginClassImplementation(ClassFinder type, Object delegateInstance) {
        if (type == null) throw new NullPointerException("type");
        if (delegateInstance == null) throw new NullPointerException("delegateInstance");

        this.type = type;
        this.delegateInstance = delegateInstance;
        this.instanceRef = new AtomicReference<Object>(null);
    }

    public Object tryGetAsPluginClass() {
        Object result = instanceRef.get();
        if (result == null) {
            instanceRef.compareAndSet(null, tryCreateProxyInstance());
            result = instanceRef.get();
        }
        return result;
    }

    private Object tryCreateProxyInstance() {
        Class<?> pluginClass = type.tryGetClass();
        if (pluginClass == null) {
            return null;
        }

        ClassLoader classLoader = getClass().getClassLoader();

        return Proxy.newProxyInstance(classLoader,
                new Class<?>[]{pluginClass},
                new SimpleDelegator(delegateInstance));
    }

    private static final class SimpleDelegator implements InvocationHandler {
        private final Object delegate;

        public SimpleDelegator(Object delegate) {
            this.delegate = delegate;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method delegateMethod
                    = delegate.getClass().getMethod(method.getName(), method.getParameterTypes());
            return delegateMethod.invoke(delegate, args);
        }
    }
}
