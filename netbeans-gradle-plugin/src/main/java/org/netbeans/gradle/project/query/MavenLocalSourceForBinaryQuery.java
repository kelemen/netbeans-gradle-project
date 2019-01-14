package org.netbeans.gradle.project.query;

import java.io.File;
import javax.swing.event.ChangeListener;
import org.netbeans.gradle.project.util.MavenFileUtils;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation;
import org.netbeans.spi.java.queries.SourceForBinaryQueryImplementation2;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = SourceForBinaryQueryImplementation2.class),
    @ServiceProvider(service = SourceForBinaryQueryImplementation.class)})
public final class MavenLocalSourceForBinaryQuery extends AbstractSourceForBinaryQuery {

    public MavenLocalSourceForBinaryQuery() {
    }

    @Override
    protected Result tryFindSourceRoot(File binaryRoot) {
        final FileObject binaryRootObj = FileUtil.toFileObject(binaryRoot);
        if (binaryRootObj == null) {
            return null;
        }
        
        String sourceName = MavenFileUtils.binaryToSourceName(binaryRootObj);
        if (sourceName == null) {
            return null;
        }
        File binaryRootParent = binaryRoot.getParentFile();
        File sourceFile = new File(binaryRootParent, sourceName);

        FileObject sourceFileObj = FileUtil.toFileObject(sourceFile);
        if (sourceFileObj == null) {
            return null;
        }
        
        FileObject asArchive = NbFileUtils.asArchiveOrDir(sourceFileObj);
        if (asArchive == null) {
            return null;
        }

        return new SourceForBinaryQueryImplementation2.Result() {
            @Override
            public boolean preferSources() {
                return false;
            }

            @Override
            public FileObject[] getRoots() {
                return new FileObject[]{asArchive};
            }

            @Override
            public void addChangeListener(ChangeListener l) {
            }

            @Override
            public void removeChangeListener(ChangeListener l) {
            }
        };
    }
}
