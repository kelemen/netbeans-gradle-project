package org.netbeans.gradle.project.properties;

import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.tasks.vars.StringResolver;

public final class GradleLocationDefault implements GradleLocation {
    public static final GradleLocation DEFAULT = new GradleLocationDefault();
    public static final GradleLocationRef DEFAULT_REF = new GradleLocationRef() {
        @Override
        public String getUniqueTypeName() {
            return UNIQUE_TYPE_NAME;
        }

        @Override
        public String asString() {
            return null;
        }

        @Override
        public GradleLocation getLocation(StringResolver resolver) {
            return DEFAULT;
        }
    };

    public static final String UNIQUE_TYPE_NAME = "DEFAULT";

    private GradleLocationDefault() {
    }

    @Override
    public void applyLocation(Applier applier) {
        applier.applyDefault();
    }

    @Override
    public String toLocalizedString() {
        return NbStrings.getGradleLocationDefault();
    }
}
