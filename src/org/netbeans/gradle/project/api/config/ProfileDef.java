package org.netbeans.gradle.project.api.config;

public final class ProfileDef {
    private final String groupName;
    private final String fileName;
    private final String displayName;

    public ProfileDef(String groupName, String fileName, String displayName) {
        if (fileName == null) throw new NullPointerException("fileName");
        if (displayName == null) throw new NullPointerException("displayName");

        this.groupName = groupName;
        this.fileName = fileName;
        this.displayName = displayName;
    }

    public String getGroupName() {
        return groupName;
    }

    public String getFileName() {
        return fileName;
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 67 * hash + (groupName != null ? groupName.hashCode() : 0);
        hash = 67 * hash + fileName.hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (obj == this) return true;
        if (getClass() != obj.getClass()) return false;

        final ProfileDef other = (ProfileDef)obj;
        if ((this.groupName == null) ? (other.groupName != null) : !this.groupName.equals(other.groupName)) {
            return false;
        }

        return this.fileName.equals(other.fileName);
    }
}
