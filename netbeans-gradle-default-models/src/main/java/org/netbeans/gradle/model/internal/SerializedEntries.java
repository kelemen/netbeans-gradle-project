package org.netbeans.gradle.model.internal;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import org.netbeans.gradle.model.util.SerializationUtils;

public final class SerializedEntries implements Serializable {
    private static final long serialVersionUID = 1L;

    private final byte[] serializedValues;

    public SerializedEntries(Collection<?> values) {
        this.serializedValues = SerializationUtils.serializeObject(new ArrayList<Object>(values));
    }

    public List<?> getUnserialized(ClassLoader classLoader) {
        try {
            Object result = classLoader != null
                    ? SerializationUtils.deserializeObject(serializedValues, classLoader)
                    : SerializationUtils.deserializeObject(serializedValues);
            return (List<?>)result;
        } catch (ClassNotFoundException ex) {
            throw new RuntimeException(ex);
        }
    }
}
