package org.netbeans.gradle.project.util;

import org.junit.Test;

import static org.junit.Assert.*;

public class ArrayUtilsTest {
    private static Object[] testArray(int... values) {
        Object[] result = new Object[values.length];
        for (int i = 0; i < values.length; i++) {
            result[i] = values[i];
        }
        return result;
    }

    @Test
    public void testWithZeroArray() {
        Object[] result = ArrayUtils.concatArrays();
        assertArrayEquals(testArray(), result);
    }

    @Test
    public void testWithSingleArrayEmpty() {
        Object[] result = ArrayUtils.concatArrays(testArray());
        assertArrayEquals(testArray(), result);
    }

    @Test
    public void testWithSingleArraySingleElement() {
        Object[] result = ArrayUtils.concatArrays(testArray(1));
        assertArrayEquals(testArray(1), result);
    }

    @Test
    public void testWithSingleArray() {
        Object[] result = ArrayUtils.concatArrays(testArray(1, 2, 3, 4));
        assertArrayEquals(testArray(1, 2, 3, 4), result);
    }

    @Test
    public void testWithTwoArrays() {
        Object[] result = ArrayUtils.concatArrays(testArray(1, 2, 3), testArray(4, 5, 6, 7));
        assertArrayEquals(testArray(1, 2, 3, 4, 5, 6, 7), result);
    }

    @Test
    public void testWithTwoArraysEmpty() {
        Object[] result = ArrayUtils.concatArrays(testArray(), testArray());
        assertArrayEquals(testArray(), result);
    }

    @Test
    public void testWithThreeArrays() {
        Object[] result = ArrayUtils.concatArrays(testArray(1, 2), testArray(3, 4, 5), testArray(6));
        assertArrayEquals(testArray(1, 2, 3, 4, 5, 6), result);
    }
}
