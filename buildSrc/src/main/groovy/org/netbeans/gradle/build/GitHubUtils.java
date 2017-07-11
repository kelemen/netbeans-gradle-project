package org.netbeans.gradle.build;

import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.function.Consumer;
import okhttp3.HttpUrl;
import okhttp3.Request;
import org.gradle.api.Project;

public final class GitHubUtils {
    public static final String BASE_API_URL = "https://api.github.com";
    public static final String USER_NAME = "kelemen";
    public static final String REPO_NAME = "netbeans-gradle-project";

    public static String getDefaultAuthToken(Project project) {
        return PropertyUtils.getStringProperty(project, "githubApiToken", null);
    }

    public static void sendPost(
            String url,
            String authToken,
            JsonElement message) throws IOException {
        sendPost(url, authToken, message, request -> { });
    }

    public static void sendPost(
            String url,
            String authToken,
            JsonElement message,
            Consumer<? super Request.Builder> requestConfig) throws IOException {
        HttpUtils.sendPost(HttpUrl.parse(url), message, request -> {
            request.addHeader("Authorization", "token " + authToken);
            request.addHeader("Accept", "application/vnd.github.v3+json");
            requestConfig.accept(request);
        });
    }

    public static String getGitHubUrl(String subPath) {
        return subPath.startsWith("/")
                ? BASE_API_URL + subPath
                : BASE_API_URL + '/' + subPath;
    }

    private GitHubUtils() {
        throw new AssertionError();
    }
}
