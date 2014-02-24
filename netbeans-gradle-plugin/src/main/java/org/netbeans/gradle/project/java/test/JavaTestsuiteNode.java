package org.netbeans.gradle.project.java.test;

import java.awt.event.ActionEvent;
import java.io.File;
import javax.swing.AbstractAction;
import javax.swing.Action;
import org.netbeans.gradle.model.java.JavaSourceGroup;
import org.netbeans.gradle.model.java.JavaSourceSet;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.java.JavaExtension;
import org.netbeans.gradle.project.output.OpenEditorOutputListener;
import org.netbeans.modules.gsf.testrunner.api.TestsuiteNode;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

public final class JavaTestsuiteNode extends TestsuiteNode {
    private static final String[] EXTENSIONS = {".java", ".groovy", ".scala"};

    private final JavaExtension javaExt;

    public JavaTestsuiteNode(String suiteName, boolean filtered, JavaExtension javaExt) {
        super(suiteName, filtered);

        if (javaExt == null) throw new NullPointerException("javaExt");

        this.javaExt = javaExt;
    }

    @Override
    public Action[] getActions(boolean context) {
        return new Action[] {
            getJumpToSourcesAction(),
        };
    }

    @Override
    public Action getPreferredAction() {
        return getJumpToSourcesAction();
    }

    private String getTestClassName() {
        int nestedClassSeparator = suiteName.indexOf('$');
        return nestedClassSeparator >= 0
                ? suiteName.substring(0, nestedClassSeparator)
                : suiteName;
    }

    private static FileObject tryGetTestFile(File root, String relPath) {
        FileObject rootObj = FileUtil.toFileObject(root);
        if (rootObj == null) {
            return null;
        }

        for (String extension: EXTENSIONS) {
            FileObject sourceFile = rootObj.getFileObject(relPath + extension);
            if (sourceFile != null) {
                return sourceFile;
            }
        }
        return null;
    }

    private static FileObject tryGetTestFile(JavaSourceGroup sourceGroup, String relPath) {
        for (File root: sourceGroup.getSourceRoots()) {
            FileObject result = tryGetTestFile(root, relPath);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private static FileObject tryGetTestFile(JavaSourceSet sourceSet, String relPath) {
        for (JavaSourceGroup sourceGroup: sourceSet.getSourceGroups()) {
            FileObject result = tryGetTestFile(sourceGroup, relPath);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private FileObject tryGetTestFile() {
        String testClassName = getTestClassName();
        // Note that we always need '/' for FileObject (no matter the file system).
        String relPath = testClassName.replace('.', '/');

        for (JavaSourceSet sourceSet: javaExt.getCurrentModel().getMainModule().getSources()) {
            FileObject result = tryGetTestFile(sourceSet, relPath);
            if (result != null) {
                return result;
            }
        }
        return null;
    }

    private Action getJumpToSourcesAction() {
        return new JumpToSourcesAction();
    }

    private void jumpToSourcesNow() {
        FileObject testFile = tryGetTestFile();
        if (testFile == null) {
            return;
        }

        OpenEditorOutputListener.tryOpenFile(testFile, -1);
    }

    @SuppressWarnings("serial")
    private class JumpToSourcesAction extends AbstractAction {
        public JumpToSourcesAction() {
            super(NbStrings.getJumpToSource());
        }

        @Override
        public void actionPerformed(ActionEvent e) {
            NbGradleProject.PROJECT_PROCESSOR.execute(new Runnable() {
                @Override
                public void run() {
                    jumpToSourcesNow();
                }
            });
        }
    }
}
