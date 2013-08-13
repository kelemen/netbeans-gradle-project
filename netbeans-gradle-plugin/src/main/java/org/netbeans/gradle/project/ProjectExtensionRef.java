package org.netbeans.gradle.project;

import javax.annotation.Nonnull;
import org.netbeans.gradle.project.api.entry.GradleProjectExtension;
import org.openide.util.Parameters;

public final class ProjectExtensionRef {
    private final GradleProjectExtension extension;

    private final String name;
    private final Class<?> extensionClass;

    public ProjectExtensionRef(@Nonnull GradleProjectExtension extension) {
        Parameters.notNull("extension", extension);
        this.extension = extension;

        this.name = extension.getExtensionName();
        this.extensionClass = extension.getClass();
    }

    @Nonnull
    public GradleProjectExtension getExtension() {
        return extension;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.name != null ? this.name.hashCode() : 0);
        hash = 31 * hash + (this.extensionClass != null ? this.extensionClass.hashCode() : 0);
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (this == obj) return true;
        if (getClass() != obj.getClass()) return false;

        final ProjectExtensionRef other = (ProjectExtensionRef)obj;
        if ((this.name == null) ? (other.name != null) : !this.name.equals(other.name)) {
            return false;
        }
        if (this.extensionClass != other.extensionClass && (this.extensionClass == null || !this.extensionClass.equals(other.extensionClass))) {
            return false;
        }
        return true;

    }
}
