package org.netbeans.gradle.project.properties;

import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.Collection;
import java.util.regex.Pattern;
import org.netbeans.api.java.platform.JavaPlatform;
import org.openide.modules.SpecificationVersion;

public abstract class AbstractProjectProperties implements ProjectProperties {
    public static final Charset DEFAULT_SOURCE_ENCODING = Charset.forName("UTF-8");

    public static String getSourceLevelFromPlatform(JavaPlatform platform) {
        SpecificationVersion version = platform.getSpecification().getVersion();
        String[] versionParts = version.toString().split(Pattern.quote("."));
        if (versionParts.length < 2) {
            return "1.7";
        }
        else {
            return versionParts[0] + "." + versionParts[1];
        }
    }

    @Override
    public final Collection<MutableProperty<?>> getAllProperties() {
        return Arrays.<MutableProperty<?>>asList(
                getPlatform(),
                getSourceEncoding(),
                getSourceLevel());
    }
}
