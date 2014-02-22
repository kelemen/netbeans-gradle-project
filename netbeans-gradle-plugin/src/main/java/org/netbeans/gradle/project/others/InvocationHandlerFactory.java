package org.netbeans.gradle.project.others;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;

public interface InvocationHandlerFactory {
    public InvocationHandler tryGetInvocationHandler(Object proxy, Method method, Object[] args);
}
