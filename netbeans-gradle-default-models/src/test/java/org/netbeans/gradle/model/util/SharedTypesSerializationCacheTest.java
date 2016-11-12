package org.netbeans.gradle.model.util;

import java.io.Serializable;
import org.junit.Test;

import static org.junit.Assert.*;

public class SharedTypesSerializationCacheTest {
    @Test
    public void testGetMultipleTimes() {
        SerializationCache cache = new SharedTypesSerializationCache(TestType.class);

        String value = "TEST-ORIG";

        TestType origObj = new TestType(value);

        Object firstGetValue = cache.getCached(origObj);
        assertSame(origObj, firstGetValue);

        Object secondGetValue = cache.getCached(new TestType(value));
        assertSame(origObj, secondGetValue);
    }

    @Test
    public void testGetNonEqual() {
        SerializationCache cache = new SharedTypesSerializationCache(TestType.class);

        TestType origObj = new TestType("TEST-ORIG");
        Object firstGetValue = cache.getCached(origObj);
        assertSame(origObj, firstGetValue);

        TestType newObj = new TestType("TEST-MOD");
        Object secondGetValue = cache.getCached(newObj);
        assertSame(newObj, secondGetValue);
    }

    @Test
    public void testGetUnknownType() {
        SerializationCache cache = new SharedTypesSerializationCache(TestType.class);

        Object origObj = new Object();

        Object firstGetValue = cache.getCached(origObj);
        assertSame(origObj, firstGetValue);

        Object newObj = new Object();
        Object secondGetValue = cache.getCached(newObj);
        assertSame(newObj, secondGetValue);
    }

    private static final class TestType implements Serializable {
        private static final long serialVersionUID = 1L;

        private final String value;

        public TestType(String value) {
            this.value = value;
        }

        @Override
        public int hashCode() {
            int hash = 5;
            hash = 11 * hash + (value != null ? value.hashCode() : 0);
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (this == obj) return true;
            if (obj == null) return false;
            if (getClass() != obj.getClass()) return false;

            final TestType other = (TestType)obj;
            return this.value == null
                    ? other.value == null
                    : this.value.equals(other.value);
        }

        @Override
        public String toString() {
            return value;
        }
    }
}
