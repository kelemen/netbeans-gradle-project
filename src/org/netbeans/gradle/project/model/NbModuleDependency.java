package org.netbeans.gradle.project.model;

public final class NbModuleDependency implements NbDependency {
    private final NbGradleModule module;
    private final boolean transitive;

    public NbModuleDependency(NbGradleModule module, boolean transitive) {
        if (module == null) throw new NullPointerException("module");

        this.module = module;
        this.transitive = transitive;
    }

    public NbGradleModule getModule() {
        return module;
    }

    @Override
    public String getShortName() {
        return module.getName();
    }

    @Override
    public boolean isTransitive() {
        return transitive;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + module.getUniqueName().hashCode();
        return hash;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final NbModuleDependency other = (NbModuleDependency)obj;
        return this.module.getUniqueName().equals(other.module.getUniqueName());
    }
}
