package org.netbeans.gradle.project.output;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openide.cookies.EditorCookie;
import org.openide.filesystems.FileObject;
import org.openide.filesystems.FileUtil;
import org.openide.loaders.DataObject;
import org.openide.loaders.DataObjectNotFoundException;
import org.openide.text.Line;
import org.openide.windows.OutputEvent;
import org.openide.windows.OutputListener;

public final class OpenEditorOutputListener implements OutputListener, ActionListener, Runnable {
    private static final Logger LOGGER = Logger.getLogger(OpenEditorOutputListener.class.getName());

    private final EditorCookie editor;
    private final int lineNumber;

    public OpenEditorOutputListener(EditorCookie editor, int lineNumber) {
        this.editor = Objects.requireNonNull(editor, "editor");
        this.lineNumber = lineNumber;
    }

    public static OpenEditorOutputListener tryCreateListener(File file, int lineNumber) {
        Objects.requireNonNull(file, "file");

        File normFile = FileUtil.normalizeFile(file);
        if (normFile == null) {
            return null;
        }

        FileObject fileObj = FileUtil.toFileObject(normFile);
        return fileObj != null ? tryCreateListener(fileObj, lineNumber) : null;
    }

    public static OpenEditorOutputListener tryCreateListener(FileObject fileObj, int lineNumber) {
        Objects.requireNonNull(fileObj, "fileObj");

        try {
            DataObject dataObj = DataObject.find(fileObj);
            if (dataObj == null) {
                return null;
            }

            EditorCookie editor = dataObj.getLookup().lookup(EditorCookie.class);
            if (editor == null) {
                LOGGER.log(Level.WARNING, "EditorCookie cannot be found for file: {0}", fileObj);
                return null;
            }

            return new OpenEditorOutputListener(editor, lineNumber);
        } catch (DataObjectNotFoundException ex) {
            LOGGER.log(Level.INFO, "Failed to find DataObject for file.", ex);
        }

        return null;
    }

    @Override
    public void outputLineSelected(OutputEvent ev) {
    }

    @Override
    public void outputLineAction(OutputEvent ev) {
        openInEditor();
    }

    private void openInEditor() {
        if (lineNumber <= 0) {
            editor.open();
        }
        else {
            try {
                editor.getLineSet().getCurrent(lineNumber - 1).show(Line.ShowOpenType.OPEN, Line.ShowVisibilityType.FOCUS);
            } catch (IndexOutOfBoundsException ex) {
                // In case the lineNumber is invalid
                LOGGER.warning("Invalid line number in the document to be opened.");
                editor.open();
            }
        }
    }

    @Override
    public void outputLineCleared(OutputEvent ev) {
    }

    public static boolean tryOpenFile(FileObject file, int lineNumber) {
        OpenEditorOutputListener listener = tryCreateListener(file, lineNumber);
        if (listener == null) {
            return false;
        }

        listener.openInEditor();
        return true;
    }

    @Override
    public void actionPerformed(ActionEvent e) {
        openInEditor();
    }

    @Override
    public void run() {
        openInEditor();
    }
}
