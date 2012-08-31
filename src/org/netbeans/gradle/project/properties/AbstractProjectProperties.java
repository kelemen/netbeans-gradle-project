package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;

public abstract class AbstractProjectProperties implements ProjectProperties {
    public static final Charset DEFAULT_SOURCE_ENCODING = Charset.forName("UTF-8");

    @Override
    public final Collection<MutableProperty<?>> getAllProperties() {
        return Arrays.<MutableProperty<?>>asList(
                getPlatform(),
                getSourceEncoding(),
                getSourceLevel());
    }
}
