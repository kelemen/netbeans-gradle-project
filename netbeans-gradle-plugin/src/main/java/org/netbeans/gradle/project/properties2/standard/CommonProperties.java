package org.netbeans.gradle.project.properties2.standard;

import org.jtrim.property.PropertyFactory;
import org.jtrim.property.PropertySource;
import org.netbeans.api.java.platform.JavaPlatform;
import org.netbeans.gradle.project.properties2.PropertyValueDef;
import org.netbeans.gradle.project.properties2.ValueMerger;
import org.netbeans.gradle.project.properties2.ValueReference;

public final class CommonProperties {
    public static PropertyValueDef<String, String> getIdentityValueDef() {
        return IdentityValueDef.INSTANCE;
    }

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

    private enum IdentityValueDef implements PropertyValueDef<String, String> {
        INSTANCE;

        @Override
        public PropertySource<String> property(String valueKey) {
            return PropertyFactory.constSource(valueKey);
        }

        @Override
        public String getKeyFromValue(String value) {
            return value;
        }
    }
}
