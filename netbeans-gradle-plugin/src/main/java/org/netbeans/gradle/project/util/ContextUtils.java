package org.netbeans.gradle.project.util;

import org.netbeans.api.project.FileOwnerQuery;
import org.netbeans.api.project.Project;
import org.netbeans.gradle.project.NbGradleProject;
import org.openide.filesystems.FileObject;
import org.openide.loaders.DataObject;
import org.openide.util.Lookup;

public final class ContextUtils {
    public static NbGradleProject tryGetGradleProjectFromContext(Lookup context) {
        Project project = tryGetProjectFromContext(context);
        return project.getLookup().lookup(NbGradleProject.class);
    }

    public static Project tryGetProjectFromContext(Lookup context) {
        Project project = context.lookup(Project.class);
        if (project != null) {
            return project;
        }

        FileObject fileObj = tryGetFileObjFromContext(context);
        if (fileObj != null) {
            Project owner = FileOwnerQuery.getOwner(fileObj);
            if (owner != null) {
                return owner;
            }
        }

        return null;
    }

    public static FileObject tryGetFileObjFromContextPreferData(Lookup context) {
        FileObject fileObj = null;
        DataObject dataObj = context.lookup(DataObject.class);
        if (dataObj != null) {
            fileObj = dataObj.getPrimaryFile();
        }

        if (fileObj == null) {
            fileObj = context.lookup(FileObject.class);
        }

        return fileObj;
    }

    public static FileObject tryGetFileObjFromContext(Lookup context) {
        FileObject fileObj = context.lookup(FileObject.class);
        if (fileObj != null) {
            return fileObj;
        }

        DataObject dataObj = context.lookup(DataObject.class);
        if (dataObj != null) {
            return dataObj.getPrimaryFile();
        }

        return null;
    }

    private ContextUtils() {
        throw new AssertionError();
    }
}
