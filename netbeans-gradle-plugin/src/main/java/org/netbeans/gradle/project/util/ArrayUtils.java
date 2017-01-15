package org.netbeans.gradle.project.util;

public final class ArrayUtils {
    public static Object[] concatArrays(Object[]... arrays) {
        int length = 0;
        for (Object[] array: arrays) {
            length += array.length;
        }

        Object[] result = new Object[length];
        int offset = 0;
        for (Object[] array: arrays) {
            System.arraycopy(array, 0, result, offset, array.length);
            offset += array.length;
        }
        return result;
    }

    private ArrayUtils() {
        throw new AssertionError();
    }
}
