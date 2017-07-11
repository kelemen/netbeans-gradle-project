package org.netbeans.gradle.build;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import java.io.IOException;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public final class HttpUtils {
    private static final String USER_AGENT = "netbeans-gradle-plugin";

    private static MediaType utf8MediaType(String type) {
        return MediaType.parse(type + ";charset=utf-8");
    }

    public static void sendPost(
            HttpUrl url,
            JsonElement body,
            Consumer<? super Request.Builder> requestConfig) throws IOException {
        Request.Builder request = new Request.Builder();

        request.addHeader("User-Agent", USER_AGENT);

        request.url(url);

        String content = new Gson().toJson(body);
        request.post(RequestBody.create(utf8MediaType("application/json"), content));

        requestConfig.accept(request);

        OkHttpClient client = getClient();
        Call call = client.newCall(request.build());
        try (Response response = call.execute()) {
            if (!response.isSuccessful()) {
                throw new IOException("Failed http request with " + response.code() + " for " + url);
            }
        }
    }

    public static OkHttpClient getClient() {
        OkHttpClient.Builder clientBuilder = new OkHttpClient.Builder();
        return clientBuilder.build();
    }

    private HttpUtils() {
        throw new AssertionError();
    }
}
