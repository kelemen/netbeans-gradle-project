package org.netbeans.gradle.model.util;

import java.io.IOException;
import java.lang.reflect.Method;
import org.gradle.api.JavaVersion;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assume;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import static org.junit.Assert.*;

/**
 *
 * @author Kelemen Attila
 */
public class TransferableExceptionWrapperTest {
    @BeforeClass
    public static void setUpClass() {
    }

    @AfterClass
    public static void tearDownClass() {
    }

    @Before
    public void setUp() {
    }

    @After
    public void tearDown() {
    }

    private static void checkSerializable(TransferableExceptionWrapper obj) throws ClassNotFoundException {
        byte[] serialized = SerializationUtils.serializeObject(obj);
        TransferableExceptionWrapper deserialized
                = (TransferableExceptionWrapper)SerializationUtils.deserializeObject(serialized, SerializationCache.NO_CACHE);

        assertEquals(obj.getMessage(), deserialized.getMessage());
        assertEquals(obj.getOriginalClassName(), deserialized.getOriginalClassName());
        assertEquals(obj.getOriginalMessage(), deserialized.getOriginalMessage());
        assertArrayEquals(obj.getStackTrace(), deserialized.getStackTrace());
    }

    private void checkWrapperOrSame(Throwable original, Throwable wrapper) {
        assertArrayEquals(original.getStackTrace(), wrapper.getStackTrace());
        String message = wrapper.getMessage();
        assertTrue(message.equals(original.getMessage())
                || (message.contains(original.getMessage()) && message.contains(original.getClass().getName())));
    }

    private void checkWrapper(Throwable original, TransferableExceptionWrapper wrapper) throws ClassNotFoundException {
        assertEquals(original.getMessage(), wrapper.getOriginalMessage());
        assertEquals(original.getClass().getName(), wrapper.getOriginalClassName());
        checkWrapperOrSame(original, wrapper);

        String message = wrapper.getMessage();
        assertTrue(message.contains(original.getMessage()) && message.contains(original.getClass().getName()));

        checkSerializable(wrapper);
    }

    @Test
    public void testWrapSingle() throws ClassNotFoundException {
        IOException wrapped = new IOException("testWrapSimple-Message");
        TransferableExceptionWrapper wrapper = TransferableExceptionWrapper.wrap(wrapped);

        checkWrapper(wrapped, wrapper);
    }

    @Test
    public void testWrapWithCauses() throws ClassNotFoundException {
        IllegalArgumentException wrappedCause = new IllegalArgumentException("testWrapWithCauses-cause");

        String message = "testWrapWithCauses-Message";
        IOException wrapped = new IOException(message);
        wrapped.initCause(wrappedCause);

        TransferableExceptionWrapper wrapper = TransferableExceptionWrapper.wrap(wrapped);

        checkWrapper(wrapped, wrapper);
        checkWrapperOrSame(wrapped.getCause(), wrapper.getCause());
    }

    @Test
    public void testWrapNotSerializable() throws ClassNotFoundException {
        Exception wrapped = new NotSerializableTestException("testWrapNotSerializable-Message");
        TransferableExceptionWrapper wrapper = TransferableExceptionWrapper.wrap(wrapped);

        checkWrapper(wrapped, wrapper);
    }

    @Test
    public void testWithSuppressed() throws Exception {
        // Java 7 only test.
        Assume.assumeTrue("This test is for Java7 only", JavaVersion.current().isJava7Compatible());

        IOException wrapped = new IOException("testWithSuppressed-Message");
        Method addSuppressed = ReflectionUtils.tryGetPublicMethod(
                wrapped.getClass(), "addSuppressed", Void.TYPE, Throwable.class);
        assertNotNull("Expected to have addSuppressed in Java 7", addSuppressed);

        Exception suppressed1 = new NotSerializableTestException("testWithSuppressed-suppressed1");
        Exception suppressed2 = new IOException("testWithSuppressed-suppressed2");
        addSuppressed.invoke(wrapped, suppressed1);
        addSuppressed.invoke(wrapped, suppressed2);

        TransferableExceptionWrapper wrapper = TransferableExceptionWrapper.wrap(wrapped);
        checkWrapper(wrapped, wrapper);

        Method getSuppressed = ReflectionUtils.tryGetPublicMethod(
                wrapper.getClass(), "getSuppressed", Throwable[].class);
        assertNotNull("Expected to have getSuppressed in Java 7", getSuppressed);

        Throwable[] wrappedSuppressed = (Throwable[])getSuppressed.invoke(wrapper);
        assertEquals("Must wrapp suppressed exceptions as well.", 2, wrappedSuppressed.length);

        checkWrapperOrSame(suppressed1, wrappedSuppressed[0]);
        checkWrapperOrSame(suppressed2, wrappedSuppressed[1]);
    }

    @SuppressWarnings("serial")
    private static final class NotSerializableTestException extends Exception {
        public final Object object; // Assign an unserializable object.

        public NotSerializableTestException(String message) {
            super(message);

            this.object = new Object();
        }
    }
}
