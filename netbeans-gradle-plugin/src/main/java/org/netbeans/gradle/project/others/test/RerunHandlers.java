package org.netbeans.gradle.project.others.test;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import org.netbeans.gradle.project.others.InvocationHandlerFactory;
import org.netbeans.gradle.project.others.PluginClassImplementation;

public final class RerunHandlers {
    public static Object tryCreateRerunHandler(RerunHandler instance) {
        if (instance == null) throw new NullPointerException("instance");

        PluginClassImplementation impl = new PluginClassImplementation(
                GradleTestSession.RERUN_HANDLER, instance, new EnabledHandler(instance));
        return impl.tryGetAsPluginClass();
    }

    private static final class EnabledHandler
    implements
            InvocationHandlerFactory,
            InvocationHandler {

        private final Object instance;

        public EnabledHandler(Object instance) {
            this.instance = instance;
        }


        @Override
        public InvocationHandler tryGetInvocationHandler(Object proxy, Method method, Object[] args) {
            if ("enabled".equals(method.getName()) && args.length == 1) {
                return this;
            }
            return null;
        }

        @Override
        public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
            Method instanceMethod = instance.getClass().getMethod(method.getName(), Object.class);
            return instanceMethod.invoke(instance, args);
        }
    }

    private RerunHandlers() {
        throw new AssertionError();
    }
}
