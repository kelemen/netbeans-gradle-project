package org.netbeans.gradle.project.model;

public interface ModelRefreshListener {
    public void startRefresh();
    public void endRefresh(boolean extensionsChanged);
}
