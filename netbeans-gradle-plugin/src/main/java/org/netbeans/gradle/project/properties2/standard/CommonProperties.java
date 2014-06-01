package org.netbeans.gradle.project.properties2.standard;

import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.properties2.ValueMerger;
import org.netbeans.gradle.project.properties2.ValueReference;

public final class CommonProperties {
    public static ValueMerger<JavaPlatform> getParentIfNullValueMerger() {
        return ParentIfNullValueMerger.INSTANCE;
    }

    private CommonProperties() {
        throw new AssertionError();
    }

    private enum ParentIfNullValueMerger implements ValueMerger<JavaPlatform> {
        INSTANCE;

        @Override
        public JavaPlatform mergeValues(JavaPlatform child, ValueReference<JavaPlatform> parent) {
            return child != null ? child : parent.getValue();
        }
    }
}
