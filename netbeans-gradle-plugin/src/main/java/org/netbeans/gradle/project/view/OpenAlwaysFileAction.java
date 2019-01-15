package org.netbeans.gradle.project.view;

import java.awt.event.ActionEvent;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;
import java.util.function.Supplier;
import javax.swing.AbstractAction;
import javax.swing.SwingUtilities;
import org.netbeans.gradle.project.NbStrings;
import org.netbeans.gradle.project.output.OpenEditorOutputListener;
import org.netbeans.gradle.project.script.CommonScripts;
import org.netbeans.gradle.project.script.ScriptFileProvider;
import org.netbeans.gradle.project.util.NbFileUtils;
import org.netbeans.gradle.project.util.NbTaskExecutors;
import org.openide.DialogDisplayer;
import org.openide.NotifyDescriptor;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;

@SuppressWarnings("serial")
public final class OpenAlwaysFileAction extends AbstractAction {
    private final Supplier<? extends Path> fileRef;

    public OpenAlwaysFileAction(Path file) {
        this(NbStrings.getOpenFileCaption(NbFileUtils.getFileNameStr(file)), file);
    }

    public OpenAlwaysFileAction(String name, final Path file) {
        this(name, () -> file);

        Objects.requireNonNull(file, "file");
    }

    public OpenAlwaysFileAction(String name, Supplier<? extends Path> fileRef) {
        super(name);

        this.fileRef = Objects.requireNonNull(fileRef, "fileRef");
    }

    public static OpenAlwaysFileAction openScriptAction(
            final Path baseDir,
            final String scriptBaseName,
            final ScriptFileProvider scriptProvider) {
        Objects.requireNonNull(baseDir, "baseDir");
        Objects.requireNonNull(scriptBaseName, "scriptBaseName");
        Objects.requireNonNull(scriptProvider, "scriptProvider");

        String caption = NbStrings.getOpenFileCaption(scriptBaseName + CommonScripts.DEFAULT_SCRIPT_EXTENSION);
        final CommonScripts commonScripts = new CommonScripts(scriptProvider);
        return new OpenAlwaysFileAction(caption, () -> commonScripts.getScriptFilePath(baseDir, scriptBaseName));
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
        NotifyDescriptor d = new NotifyDescriptor.Message(message, NotifyDescriptor.ERROR_MESSAGE);
        d.setTitle(title);
        DialogDisplayer.getDefault().notify(d);
    }

    private void openFileNow() {
        final FileObject fileObj = tryGetFileObjectCreateIfNeeded();
        SwingUtilities.invokeLater(() -> {
            if (fileObj != null) {
                OpenEditorOutputListener.tryOpenFile(fileObj, -1);
            }
            else {
                showCannotCreateFile();
            }
        });
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        NbTaskExecutors.DEFAULT_EXECUTOR.execute(this::openFileNow);
    }
}
