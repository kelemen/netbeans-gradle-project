package org.netbeans.gradle.project.query;

import java.io.File;
import java.net.URL;
import javax.swing.event.ChangeListener;
import org.netbeans.api.java.queries.BinaryForSourceQuery;
import org.netbeans.api.java.queries.BinaryForSourceQuery.Result;
import org.netbeans.gradle.project.util.MavenFileUtils;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.spi.java.queries.BinaryForSourceQueryImplementation;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.util.lookup.ServiceProvider;
import org.openide.util.lookup.ServiceProviders;

@ServiceProviders({
    @ServiceProvider(service = BinaryForSourceQueryImplementation.class)})
public final class MavenLocalBinaryForSourceQuery extends AbstractBinaryForSourceQuery {

    public MavenLocalBinaryForSourceQuery() {
    }

    @Override
    protected Result tryFindBinaryRoots(File sourceRoot) {
        final FileObject sourceRootObj = FileUtil.toFileObject(sourceRoot);
        if (sourceRootObj == null) {
            return null;
        }

        final String binFileName = MavenFileUtils.sourceToBinaryName(sourceRootObj);
        if (binFileName == null) {
            return null;
        }
        if (!MavenFileUtils.isSourceFile(sourceRootObj)) {
            return null;
        }
        
        File sourceRootParent = sourceRoot.getParentFile();
        File binaryFile = new File(sourceRootParent, binFileName);
        FileObject binaryFileObj = FileUtil.toFileObject(binaryFile);
        if (binaryFileObj == null) {
            return null;
        }

        FileObject asArchive = NbFileUtils.asArchiveOrDir(binaryFileObj);
        if (asArchive == null) {
            return null;
        }
        
        return new BinaryForSourceQuery.Result() {
            @Override
            public URL[] getRoots() {
                return new URL[]{asArchive.toURL()};
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
