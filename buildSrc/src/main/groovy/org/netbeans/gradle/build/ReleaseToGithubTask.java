package org.netbeans.gradle.build;

import com.google.gson.JsonObject;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.InputFile;
import org.gradle.api.tasks.TaskAction;

public class ReleaseToGithubTask extends DefaultTask {
    private String version;
    private File notesFile;
    private Boolean preRelease;
    private String authToken;

    public ReleaseToGithubTask() {
        this.version = null;
        this.notesFile = null;
        this.preRelease = null;
        this.authToken = null;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    @Input
    public String getVersion() {
        return version != null ? version : getDefaultVersion();
    }

    private String getDefaultVersion() {
        return VersionUtils.getReleaseVersion(getProject());
    }

    @InputFile
    public File getNotesFile() {
        return notesFile != null ? notesFile : getDefaultNotesFile();
    }

    private File getDefaultNotesFile() {
        Path dir = getProject().getRootDir().toPath().resolve("release-notes");
        return getReleaseNotesPath(dir, getVersion()).toFile();
    }

    public void setNotesFile(File notesFile) {
        this.notesFile = notesFile;
    }

    private static Path getReleaseNotesPath(Path dir, String version) {
        return dir.resolve("v" + version + ".md");
    }

    @Input
    public boolean isPreRelease() {
        return preRelease != null ? preRelease : getDefaultPreRelease();
    }

    private boolean getDefaultPreRelease() {
        return PropertyUtils.getBoolProperty(getProject(), "githubPreRelease", false);
    }

    public void setPreRelease(boolean preRelease) {
        this.preRelease = preRelease;
    }

    public String getAuthToken() {
        return authToken != null ? authToken : getDefaultAuthToken();
    }

    private String getDefaultAuthToken() {
        return GitHubUtils.getDefaultAuthToken(getProject());
    }

    public void setAuthToken(String authToken) {
        this.authToken = authToken;
    }

    @TaskAction
    public void upload() throws Exception {
        String appliedAuthToken = getAuthToken();
        if (appliedAuthToken == null) {
            throw new IllegalStateException("Missing API token for GitHub.");
        }

        String appliedVersion = getVersion();
        String title = "Version " + appliedVersion;
        ReleaseNotes notes = ReleaseNotes.readReleaseNotes(getNotesFile().toPath());

        String tagName = VersionUtils.getTagForVersion(appliedVersion);
        String commitId = getCommitForTag(tagName);

        JsonObject message = new JsonObject();
        message.addProperty("tag_name", tagName);
        message.addProperty("target_commitish", commitId);
        message.addProperty("name", title);
        message.addProperty("body", notes.getMarkdownBody());
        message.addProperty("draft", false);
        message.addProperty("prerelease", isPreRelease());

        String url = GitHubUtils.getGitHubUrl("repos/" + GitHubUtils.USER_NAME + '/' + GitHubUtils.REPO_NAME + "/releases");
        GitHubUtils.sendPost(url, appliedAuthToken, message);
    }

    private String getCommitForTag(String tagName) throws IOException {
        try (GitWrapper git = new GitWrapper(getProject())) {
            String commitId = git.tryGetCommitFor(tagName);
            if (commitId != null) {
                return commitId;
            }
            commitId = git.tryGetCommitFor("HEAD");
            if (commitId != null) {
                return commitId;
            }
        }

        throw new IllegalStateException("Cannot find commit for version: " + tagName);
    }
}
