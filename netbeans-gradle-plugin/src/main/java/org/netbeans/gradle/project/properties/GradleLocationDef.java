package org.netbeans.gradle.project.properties;

import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import javax.annotation.Nonnull;
import org.netbeans.gradle.project.tasks.vars.StringResolver;

public final class GradleLocationDef {
    public static final GradleLocationDef DEFAULT = new GradleLocationDef(GradleLocationDefault.DEFAULT_REF, false);

    private static final String PREFER_WRAPPER_KEY = "W";

    private final GradleLocationRef locationRef;
    private final boolean preferWrapper;

    public GradleLocationDef(GradleLocationRef locationRef, boolean preferWrapper) {
        Objects.requireNonNull(locationRef, "locationRef");

        // Performance hack: as is now we won't pass different resolvers, so we can cache the location value.
        this.locationRef = cachedRef(locationRef);
        this.preferWrapper = preferWrapper;
    }

    private static GradleLocationRef cachedRef(final GradleLocationRef src) {
        return new GradleLocationRef() {
            private final AtomicReference<GradleLocation> cache = new AtomicReference<>(null);

            @Override
            public String getUniqueTypeName() {
                return src.getUniqueTypeName();
            }

            @Override
            public String asString() {
                return src.asString();
            }

            @Override
            public GradleLocation getLocation(StringResolver resolver) {
                GradleLocation result = cache.get();
                if (result == null) {
                    result = src.getLocation(resolver);
                    if (!cache.compareAndSet(null, result)) {
                        result = cache.get();
                    }
                }
                return result;
            }
        };
    }

    public static GradleLocationDef fromVersion(String versionStr, boolean preferWrapper) {
        return new GradleLocationDef(GradleLocationVersion.getLocationRef(versionStr), preferWrapper);
    }

    private static void appendKeyValue(String key, String value, StringBuilder result) {
        result.append('?');
        result.append(key);
        result.append('=');
        result.append(value);
    }

    private static void appendLocation(GradleLocationRef location, StringBuilder result) {
        String value = location.asString();
        if (value == null) {
            return;
        }

        appendKeyValue(location.getUniqueTypeName(), value, result);
    }

    public String toStringFormat() {
        StringBuilder result = new StringBuilder(64);
        if (preferWrapper) {
            appendKeyValue(PREFER_WRAPPER_KEY, "", result);
        }
        appendLocation(locationRef, result);
        return result.toString();
    }

    private static KeyValue trySplitKeyValue(String str) {
        if (!str.startsWith("?")) {
            return null;
        }

        int sepIndex = str.indexOf('=');
        if (sepIndex < 0) {
            return null;
        }

        String key = str.substring(1, sepIndex);
        String value = str.substring(sepIndex + 1, str.length());
        return new KeyValue(key, value);
    }

    private static GradleLocationRef getLocation(KeyValue keyValue) {
        String typeName = keyValue.key;
        String value = keyValue.value;

        if (GradleLocationDefault.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
            return GradleLocationDefault.DEFAULT_REF;
        }
        if (GradleLocationVersion.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
            return GradleLocationVersion.getLocationRef(value);
        }
        if (GradleLocationDirectory.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
            return GradleLocationDirectory.getLocationRef(value);
        }
        if (GradleLocationDistribution.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
            return GradleLocationDistribution.getLocationRef(value);
        }

        return null;
    }

    private static GradleLocationRef parseLocation(String str) {
        KeyValue keyValue = trySplitKeyValue(str);
        if (keyValue != null) {
            GradleLocationRef result = getLocation(keyValue);
            if (result != null) {
                return result;
            }
        }

        return getRawLocation(str);
    }

    private static GradleLocationRef getRawLocation(String locationPath) {
        if (locationPath.isEmpty()) {
            return GradleLocationDefault.DEFAULT_REF;
        }
        else {
            return GradleLocationDirectory.getLocationRef(locationPath);
        }
    }

    public static GradleLocationDef parseFromString(String locationDefStr) {
        Objects.requireNonNull(locationDefStr, "locationDefStr");

        String normDef = locationDefStr.trim();

        KeyValue keyValue = trySplitKeyValue(normDef);
        if (keyValue == null) {
            return new GradleLocationDef(getRawLocation(normDef), false);
        }

        GradleLocationRef locationRef;
        boolean preferWrapper;
        if (keyValue.key.equalsIgnoreCase(PREFER_WRAPPER_KEY)) {
            locationRef = parseLocation(keyValue.value);
            preferWrapper = true;
        }
        else {
            preferWrapper = false;
            locationRef = getLocation(keyValue);
            if (locationRef == null) {
                locationRef = getRawLocation(normDef);
            }
        }
        return new GradleLocationDef(locationRef, preferWrapper);
    }

    @Nonnull
    public GradleLocationRef getLocationRef() {
        return locationRef;
    }

    @Nonnull
    public GradleLocation getLocation(StringResolver resolver) {
        return locationRef.getLocation(resolver);
    }

    public boolean isPreferWrapper() {
        return preferWrapper;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.locationRef);
        hash = 97 * hash + (this.preferWrapper ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final GradleLocationDef other = (GradleLocationDef)obj;

        return Objects.equals(this.locationRef, other.locationRef)
                && this.preferWrapper == other.preferWrapper;
    }

    private static final class KeyValue {
        public final String key;
        public final String value;

        public KeyValue(String key, String value) {
            this.key = key;
            this.value = value;
        }
    }
}
