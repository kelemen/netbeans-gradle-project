package org.gradle.plugins.nbm;

public final class NbmKeyStoreDef {
    private Object keyStoreFile;
    private String username;
    private String password;

    public NbmKeyStoreDef() {
        this.keyStoreFile = null;
        this.username = null;
        this.password = null;
    }

    public Object getKeyStoreFile() {
        return keyStoreFile;
    }

    public void setKeyStoreFile(Object keyStoreFile) {
        this.keyStoreFile = keyStoreFile;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
