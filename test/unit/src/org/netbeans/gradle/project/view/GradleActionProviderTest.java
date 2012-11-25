package org.netbeans.gradle.project.view;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.List;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.openide.filesystems.FileChangeListener;
import org.openide.filesystems.FileLock;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileStateInvalidException;
import org.openide.filesystems.FileSystem;
import org.openide.util.Lookup;

/**
 *
 * @author Tadas Subonis <tadas.subonis@gmail.com>
 */
public class GradleActionProviderTest {

    @BeforeClass
    public static void setUpClass() {
    }

    @Before
    public void setUp() {
    }

    /**
     * Test of getSupportedActions method, of class GradleActionProvider.
     */
    @Test
    public void should_getJavaFileOfContext_return_java_action() {
        System.out.println("should_getJavaFileOfContext_return_java_action");
        Lookup n = Lookup.EMPTY;
        TestGradleActionProvider gradleActionProvider = new TestGradleActionProvider("java");
        FileObject javaFileOfContext = gradleActionProvider.getJavaFileOfContext(n);
        assertNotNull(javaFileOfContext);
    }

    @Test
    public void should_getJavaFileOfContext_return_groovy_action() {
        System.out.println("should_getJavaFileOfContext_return_groovy_action");
        Lookup n = Lookup.EMPTY;
        TestGradleActionProvider gradleActionProvider = new TestGradleActionProvider("groovy");
        FileObject javaFileOfContext = gradleActionProvider.getJavaFileOfContext(n);
        assertNotNull(javaFileOfContext);
    }

    @Test
    public void should_getJavaFileOfContext_return_other_action() {
        System.out.println("should_getJavaFileOfContext_return_other_action");
        Lookup n = Lookup.EMPTY;
        TestGradleActionProvider gradleActionProvider = new TestGradleActionProvider("rb");
        FileObject javaFileOfContext = gradleActionProvider.getJavaFileOfContext(n);
        assertNull(javaFileOfContext);
    }

    private static class MyFileObject extends FileObject {

        private String ext;

        public MyFileObject(String ext) {
            this.ext = ext;
        }

        @Override
        public String getExt() {
            return ext;
        }

        @Override
        public String getName() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void rename(FileLock fl, String string, String string1) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public FileSystem getFileSystem() throws FileStateInvalidException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public FileObject getParent() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isFolder() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Date lastModified() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isRoot() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isData() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isValid() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void delete(FileLock fl) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Object getAttribute(String string) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setAttribute(String string, Object o) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public Enumeration<String> getAttributes() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void addFileChangeListener(FileChangeListener fl) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void removeFileChangeListener(FileChangeListener fl) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public long getSize() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public InputStream getInputStream() throws FileNotFoundException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public OutputStream getOutputStream(FileLock fl) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public FileLock lock() throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public void setImportant(boolean bln) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public FileObject[] getChildren() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public FileObject getFileObject(String string, String string1) {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public FileObject createFolder(String string) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public FileObject createData(String string, String string1) throws IOException {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }

        @Override
        public boolean isReadOnly() {
            throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
        }
    }

    private static class TestGradleActionProvider extends GradleActionProvider {

        private String ext;

        public TestGradleActionProvider(String ext) {
            super(null);
            this.ext = ext;
        }

        @Override
        protected List<FileObject> getFilesOfContext(Lookup context) {
            List<FileObject> fileObjects = new ArrayList<FileObject>();
            FileObject fileObject = new MyFileObject(ext);
            fileObjects.add(fileObject);
            return fileObjects;
        }

        @Override
        public FileObject getJavaFileOfContext(Lookup context) {
            return super.getJavaFileOfContext(context); //To change body of generated methods, choose Tools | Templates.
        }
    }
}
