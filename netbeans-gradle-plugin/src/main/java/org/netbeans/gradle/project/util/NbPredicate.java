package org.netbeans.gradle.project.util;

public interface NbPredicate<T> {
    public static final NbPredicate<Object> TRUE = arg -> true;
    public static final NbPredicate<Object> FALSE = arg -> false;

    public boolean test(T t);
}
