package org.netbeans.gradle.build;

import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ReleaseNotes {
    private final String title;
    private final String markdownBody;

    public ReleaseNotes(String title, String markdownBody) {
        this.title = Objects.requireNonNull(title, "title");
        this.markdownBody = Objects.requireNonNull(markdownBody, "markdownBody");
    }

    public static ReleaseNotes readReleaseNotes(Path notesFile) throws IOException {
        try (Reader src = Files.newBufferedReader(notesFile, StandardCharsets.UTF_8)) {
            return parseFromMarkdown(src);
        }
    }

    public static ReleaseNotes parseFromMarkdown(Reader src) throws IOException {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        Document doc = parser.parseReader(src);

        for (Node node : doc.getChildren()) {
            if (node instanceof Heading) {
                Heading header = (Heading)node;
                String headerText = header.getText().toString().trim();
                String content = doc.getChars().baseSubSequence(header.getEndOffset(), doc.getEndOffset()).toString().trim();
                return new ReleaseNotes(headerText, content);
            }
        }

        throw new IOException("Missing header in readme.");
    }

    public String getTitle() {
        return title;
    }

    public String getMarkdownBody() {
        return markdownBody;
    }
}
