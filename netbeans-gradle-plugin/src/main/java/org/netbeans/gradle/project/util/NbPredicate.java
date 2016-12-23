package org.netbeans.gradle.project.util;

public interface NbPredicate<T> {
    public static final NbPredicate<Object> TRUE = new NbPredicate<Object>() {
        @Override
        public boolean test(Object t) {
            return true;
        }
    };

    public static final NbPredicate<Object> FALSE = new NbPredicate<Object>() {
        @Override
        public boolean test(Object t) {
            return false;
        }
    };

    public boolean test(T t);
}
