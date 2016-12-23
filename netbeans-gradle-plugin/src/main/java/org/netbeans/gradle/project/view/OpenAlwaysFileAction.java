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
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.output.OpenEditorOutputListener;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbSupplier;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

@SuppressWarnings("serial")
public final class OpenAlwaysFileAction extends AbstractAction {
    private final NbSupplier<? extends Path> fileRef;

    public OpenAlwaysFileAction(Path file) {
        this(NbStrings.getOpenFileCaption(NbFileUtils.getFileNameStr(file)), file);
    }

    public OpenAlwaysFileAction(String name, final Path file) {
        this(name, new NbSupplier<Path>() {
            @Override
            public Path get() {
                return file;
            }
        });

        ExceptionHelper.checkNotNullArgument(file, "file");
    }

    public OpenAlwaysFileAction(String name, NbSupplier<? extends Path> fileRef) {
        super(name);

        ExceptionHelper.checkNotNullArgument(fileRef, "fileRef");
        this.fileRef = fileRef;
    }

    public static OpenAlwaysFileAction openScriptAction(
            final Path baseDir,
            final String scriptBaseName,
            final ScriptFileProvider scriptProvider) {
        ExceptionHelper.checkNotNullArgument(baseDir, "baseDir");
        ExceptionHelper.checkNotNullArgument(scriptBaseName, "scriptBaseName");
        ExceptionHelper.checkNotNullArgument(scriptProvider, "scriptProvider");

        String caption = NbStrings.getOpenFileCaption(scriptBaseName + CommonScripts.DEFAULT_SCRIPT_EXTENSION);
        final CommonScripts commonScripts = new CommonScripts(scriptProvider);
        return new OpenAlwaysFileAction(caption, new NbSupplier<Path>() {
            @Override
            public Path get() {
                return commonScripts.getScriptFilePath(baseDir, scriptBaseName);
            }
        });
    }

    private FileObject tryGetFileObjectCreateIfNeeded() {
        Path file = fileRef.get();
        if (file == null) {
            return null;
        }

        if (!Files.isRegularFile(file)) {
            Path dir = file.getParent();
            if (dir != null) {
                try {
                    Files.createDirectories(dir);
                    Files.createFile(file);
                } catch (IOException ex) {
                    return null;
                }
            }
        }

        return FileUtil.toFileObject(file.toFile());
    }

    private void showCannotCreateFile() {
        // TODO: I18N
        String message = "Cannot create file: " + fileRef.get();
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
