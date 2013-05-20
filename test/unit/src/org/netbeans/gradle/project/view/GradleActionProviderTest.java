package org.netbeans.gradle.project.view;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Set;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.netbeans.gradle.project.NbGradleProject;
import org.netbeans.spi.project.ProjectState;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.filesystems.FileUtil;
import org.openide.util.Lookup;
import org.openide.util.lookup.Lookups;

import static org.junit.Assert.*;
import static org.netbeans.spi.project.ActionProvider.*;

/**
 *
 * @author Tadas Subonis <tadas.subonis@gmail.com>
 */
public class GradleActionProviderTest {

    private static NbGradleProject project;

    @BeforeClass
    public static void setUpClass() {
    }

    @Before
    public void setUp() throws IOException {
        FileObject fileObject = FileUtil.createFolder(FileUtil.normalizeFile(new File(".")));
        ProjectState projectState = new ProjectState() {
            @Override
            public void markModified() {
                throw new UnsupportedOperationException();
            }

            @Override
            public void notifyDeleted() throws IllegalStateException {
                throw new UnsupportedOperationException();
            }
        };
        project = NbGradleProject.createProject(fileObject, projectState);
    }

    private static String[] getSingleCommands() {
        return new String[] {
            COMMAND_RUN_SINGLE,
            COMMAND_DEBUG_SINGLE,
            COMMAND_TEST_SINGLE,
            COMMAND_DEBUG_TEST_SINGLE,
        };
    }

    @Test
    public void testSingleCommandsEnabled() {
        GradleActionProvider actionProvider = new GradleActionProvider(project);
        Set<String> supportedActions = new HashSet<String>(Arrays.asList(actionProvider.getSupportedActions()));

        for (String ext: Arrays.asList("groovy", "java")) {
            for (String command: getSingleCommands()) {
                Lookup lookup = Lookups.fixed(new MyFileObject(ext));
                boolean actionEnabled = actionProvider.isActionEnabled(command, lookup);
                assertTrue(actionEnabled);
                assertTrue(supportedActions.contains(command));
            }
        }
    }

    @SuppressWarnings("serial")
    private static class MyFileObject extends FileObject {
        private final String ext;

        public MyFileObject(String ext) {
            this.ext = ext;
        }

        @Override
        public String getExt() {
            return ext;
        }

        @Override
        public String getName() {
            return "myfile";
        }

        @Override
        public void rename(FileLock fl, String string, String string1) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileSystem getFileSystem() throws FileStateInvalidException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject getParent() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isFolder() {
            throw new UnsupportedOperationException();
        }

        @Override
        public Date lastModified() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isRoot() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isData() {
            throw new UnsupportedOperationException();
        }

        @Override
        public boolean isValid() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void delete(FileLock fl) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Object getAttribute(String string) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void setAttribute(String string, Object o) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public Enumeration<String> getAttributes() {
            throw new UnsupportedOperationException();
        }

        @Override
        public void addFileChangeListener(FileChangeListener fl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public void removeFileChangeListener(FileChangeListener fl) {
            throw new UnsupportedOperationException();
        }

        @Override
        public long getSize() {
            throw new UnsupportedOperationException();
        }

        @Override
        public InputStream getInputStream() throws FileNotFoundException {
            throw new UnsupportedOperationException();
        }

        @Override
        public OutputStream getOutputStream(FileLock fl) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileLock lock() throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public void setImportant(boolean bln) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject[] getChildren() {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject getFileObject(String string, String string1) {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject createFolder(String string) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        public FileObject createData(String string, String string1) throws IOException {
            throw new UnsupportedOperationException();
        }

        @Override
        @Deprecated
        public boolean isReadOnly() {
            throw new UnsupportedOperationException();
        }
    }
}
