package org.netbeans.gradle.project.properties;

import java.util.Objects;
import org.jtrim.utils.ExceptionHelper;

public final class GradleLocationDef {
    public static final GradleLocationDef DEFAULT = new GradleLocationDef(GradleLocationDefault.INSTANCE, false);

    private static final String PREFER_WRAPPER_KEY = "W";

    private final GradleLocation location;
    private final boolean preferWrapper;

    public GradleLocationDef(GradleLocation location, boolean preferWrapper) {
        ExceptionHelper.checkNotNullArgument(location, "location");

        this.location = location;
        this.preferWrapper = preferWrapper;
    }

    public static GradleLocationDef fromVersion(String versionStr, boolean preferWrapper) {
        return new GradleLocationDef(new GradleLocationVersion(versionStr), preferWrapper);
    }

    private static void appendKeyValue(String key, String value, StringBuilder result) {
        result.append('?');
        result.append(key);
        result.append('=');
        result.append(value);
    }

    private static void appendLocation(GradleLocation location, StringBuilder result) {
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
        appendLocation(location, result);
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

    private static GradleLocation getLocation(KeyValue keyValue) {
        String typeName = keyValue.key;
        String value = keyValue.value;

        if (GradleLocationDefault.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
            return GradleLocationDefault.INSTANCE;
        }
        if (GradleLocationVersion.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
            return new GradleLocationVersion(value);
        }
        if (GradleLocationDirectory.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
            return new GradleLocationDirectory(value);
        }
        if (GradleLocationDistribution.UNIQUE_TYPE_NAME.equalsIgnoreCase(typeName)) {
            return new GradleLocationDistribution(value);
        }

        return null;
    }

    private static GradleLocation parseLocation(String str) {
        KeyValue keyValue = trySplitKeyValue(str);
        if (keyValue != null) {
            GradleLocation result = getLocation(keyValue);
            if (result != null) {
                return result;
            }
        }

        return getRawLocation(str);
    }

    private static GradleLocation getRawLocation(String locationPath) {
        if (locationPath.isEmpty()) {
            return GradleLocationDefault.INSTANCE;
        }
        else {
            return new GradleLocationDirectory(locationPath);
        }
    }

    public static GradleLocationDef parseFromString(String locationDefStr) {
        ExceptionHelper.checkNotNullArgument(locationDefStr, "locationDefStr");

        String normDef = locationDefStr.trim();

        KeyValue keyValue = trySplitKeyValue(normDef);
        if (keyValue == null) {
            return new GradleLocationDef(getRawLocation(normDef), false);
        }

        GradleLocation location;
        boolean preferWrapper;
        if (keyValue.key.equalsIgnoreCase(PREFER_WRAPPER_KEY)) {
            location = parseLocation(keyValue.value);
            preferWrapper = true;
        }
        else {
            preferWrapper = false;
            location = getLocation(keyValue);
            if (location == null) {
                location = getRawLocation(normDef);
            }
        }
        return new GradleLocationDef(location, preferWrapper);
    }

    public GradleLocation getLocation() {
        return location;
    }

    public boolean isPreferWrapper() {
        return preferWrapper;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 97 * hash + Objects.hashCode(this.location);
        hash = 97 * hash + (this.preferWrapper ? 1 : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final GradleLocationDef other = (GradleLocationDef)obj;

        return Objects.equals(this.location, other.location)
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
