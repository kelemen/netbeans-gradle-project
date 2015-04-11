package org.netbeans.gradle.project.view;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import javax.swing.JOptionPane;
import javax.swing.SwingUtilities;
import org.jtrim.cancel.Cancellation;
import org.jtrim.cancel.CancellationToken;
import org.jtrim.concurrent.CancelableTask;
import org.jtrim.utils.ExceptionHelper;
import org.netbeans.gradle.project.NbTaskExecutors;
import org.netbeans.gradle.project.output.OpenEditorOutputListener;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

@SuppressWarnings("serial")
public final class OpenAlwaysFileAction extends AbstractAction {
    private final Path file;

    public OpenAlwaysFileAction(String name, Path file) {
        super(name);

        ExceptionHelper.checkNotNullArgument(file, "file");
        this.file = file;
    }

    private void createNewFile() throws IOException {
        Files.createFile(file);
    }

    private FileObject tryGetFileObjectCreateIfNeeded() {
        if (!Files.isRegularFile(file)) {
            Path dir = file.getParent();
            if (dir != null) {
                try {
                    Files.createDirectories(dir);
                    createNewFile();
                } catch (IOException ex) {
                    return null;
                }
            }
        }

        return FileUtil.toFileObject(file.toFile());
    }

    private void showCannotCreateFile() {
        // TODO: I18N
        String message = "Cannot create file: " + file;
        String title = "File open error";
        JOptionPane.showMessageDialog(null, message, title, JOptionPane.ERROR_MESSAGE);
    }

    private void openFileNow() {
        final FileObject fileObj = tryGetFileObjectCreateIfNeeded();
        SwingUtilities.invokeLater(new Runnable() {
            @Override
            public void run() {
                if (fileObj != null) {
                    OpenEditorOutputListener.tryOpenFile(fileObj, -1);
                }
                else {
                    showCannotCreateFile();
                }
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NbTaskExecutors.DEFAULT_EXECUTOR.execute(Cancellation.UNCANCELABLE_TOKEN, new CancelableTask() {
            @Override
            public void execute(CancellationToken cancelToken) throws Exception {
                openFileNow();
            }
        }, null);
    }
}
