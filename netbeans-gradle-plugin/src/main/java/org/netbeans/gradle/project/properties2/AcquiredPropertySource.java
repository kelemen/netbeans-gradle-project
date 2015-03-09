package org.netbeans.gradle.project.properties2;

import org.jtrim.property.PropertySource;

public interface AcquiredPropertySource<ValueType>
extends
        PropertySource<ValueType>,
        AutoCloseable {

    @Override
    public void close();
}
