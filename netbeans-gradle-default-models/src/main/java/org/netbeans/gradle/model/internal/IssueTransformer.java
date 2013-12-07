package org.netbeans.gradle.model.internal;

public interface IssueTransformer {
    public Object transformIssue(Throwable issue);
}
