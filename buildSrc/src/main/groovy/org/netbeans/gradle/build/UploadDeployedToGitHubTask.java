package org.netbeans.gradle.build;

import com.google.gson.JsonElement;
import java.io.IOException;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.RequestBody;
import org.gradle.api.DefaultTask;
import org.gradle.api.tasks.TaskAction;

public class UploadDeployedToGitHubTask extends DefaultTask {
    private String version;
    private String authToken;

    public UploadDeployedToGitHubTask() {
        this.version = null;
        this.authToken = null;
    }

    public String getVersion() {
        return version != null ? version : VersionUtils.getReleaseVersion(getProject());
    }

    public void setVersion(String version) {
        this.version = version;
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
        String appliedVersion = getVersion();

        String baseUploadUrl = getUploadUrl(appliedAuthToken, appliedVersion);

        HttpUrl downloadUrl = HttpUrl.parse(sourceUrl(appliedVersion));
        byte[] binaryContent = HttpUtils.downloadBytes(downloadUrl, request -> { });

        HttpUrl.Builder uploadUrl = HttpUtils.parseUrl(baseUploadUrl).newBuilder();
        uploadUrl.addQueryParameter("name", "netbeans-gradle-plugin-" + appliedVersion + ".nbm");

        GitHubUtils.sendPost(
                uploadUrl.build(),
                appliedAuthToken,
                RequestBody.create(MediaType.parse("application/octet-stream"), binaryContent),
                request -> { });
    }

    private static String getUploadUrl(String authToken, String version) throws IOException {
        String tagName = VersionUtils.getTagForVersion(version);
        String url = GitHubUtils.getGitHubUrl("repos/" + GitHubUtils.USER_NAME + "/" + GitHubUtils.REPO_NAME + "/releases/tags/" + tagName);
        JsonElement response = GitHubUtils.sendJsonGet(url, authToken);
        JsonElement uploadUrlElement = response.getAsJsonObject().get("upload_url");
        if (uploadUrlElement == null) {
            throw new IOException("Missing upload url.");
        }
        return parseUploadUrl(uploadUrlElement.getAsString());
    }

    private static String sourceUrl(String version) {
        String repoBase = "http://dl.bintray.com/kelemen/maven/com/github/kelemen/netbeans-gradle-plugin/";
        return repoBase + version + "/netbeans-gradle-plugin-" + version + ".nbm";
    }

    private static String parseUploadUrl(String raw) {
        int patternIndex = raw.lastIndexOf('{');
        if (patternIndex < 0) {
            return raw;
        }
        return raw.substring(0, patternIndex);
    }
}
