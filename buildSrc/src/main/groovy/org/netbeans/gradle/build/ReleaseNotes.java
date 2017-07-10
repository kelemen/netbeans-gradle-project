package org.netbeans.gradle.build;

import com.vladsch.flexmark.ast.Document;
import com.vladsch.flexmark.ast.Heading;
import com.vladsch.flexmark.ast.Node;
import com.vladsch.flexmark.parser.Parser;
import com.vladsch.flexmark.util.options.MutableDataSet;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Objects;

public final class ReleaseNotes {
    private final String version;
    private final String title;
    private final String markdownBody;

    public ReleaseNotes(String version, String title, String markdownBody) {
        this.version = Objects.requireNonNull(version, "version");
        this.title = Objects.requireNonNull(title, "title");
        this.markdownBody = Objects.requireNonNull(markdownBody, "markdownBody");
    }

    public static ReleaseNotes readReleaseNotes(Path dir, String version) throws IOException {
        Path file = dir.resolve("v" + version + ".md");
        try (Reader src = Files.newBufferedReader(file, StandardCharsets.UTF_8)) {
            return parseFromMarkdown(version, src);
        }
    }

    public static ReleaseNotes parseFromMarkdown(String version, Reader src) throws IOException {
        MutableDataSet options = new MutableDataSet();
        Parser parser = Parser.builder(options).build();
        Document doc = parser.parseReader(src);

        for (Node node : doc.getChildren()) {
            if (node instanceof Heading) {
                Heading header = (Heading)node;
                String headerText = header.getText().toString().trim();
                String content = doc.getChars().baseSubSequence(header.getEndOffset(), doc.getEndOffset()).toString().trim();
                return new ReleaseNotes(version, headerText, content);
            }
        }

        throw new IOException("Missing header for readme: " + version);
    }

    private static String readTitle(BufferedReader src) throws IOException {
        for (String line = src.readLine(); line != null; line = src.readLine()) {
            if (line.startsWith("# ")) {
                return line.substring(1).trim();
            }
        }
        throw new IOException("Missing title in readme.");
    }

    public String getVersion() {
        return version;
    }

    public String getTitle() {
        return title;
    }

    public String getMarkdownBody() {
        return markdownBody;
    }
}
