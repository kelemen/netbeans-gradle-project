package org.netbeans.gradle.build;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.function.Consumer;
import okhttp3.Call;
import okhttp3.HttpUrl;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import okhttp3.ResponseBody;

public final class HttpUtils {
    private static final String USER_AGENT = "netbeans-gradle-plugin";

    private static MediaType utf8MediaType(String type) {
        return MediaType.parse(type + ";charset=utf-8");
    }

    public static HttpUrl parseUrl(String url) {
        HttpUrl result = HttpUrl.parse(url);
        if (result == null) {
            throw new IllegalArgumentException("Invalid url: " + url);
        }
        return result;
    }

    public static byte[] downloadBytes(
            HttpUrl url,
            Consumer<? super Request.Builder> requestConfig) throws IOException {
        Request.Builder request = new Request.Builder();

        request.addHeader("User-Agent", USER_AGENT);

        request.url(url);
        request.get();

        requestConfig.accept(request);

        OkHttpClient client = getClient();
        Call call = client.newCall(request.build());
        try (Response response = call.execute()) {
            verifySuccess(url, response);

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("No response for " + url);
            }

            ByteArrayOutputStream result = new ByteArrayOutputStream(4 * 1024 * 1024);
            IoUtils.copyStream(body.byteStream(), result);
            return result.toByteArray();
        }
    }

    public static JsonElement sendJsonGet(
            HttpUrl url,
            Consumer<? super Request.Builder> requestConfig) throws IOException {
        Request.Builder request = new Request.Builder();

        request.addHeader("User-Agent", USER_AGENT);

        request.url(url);
        request.get();

        requestConfig.accept(request);

        OkHttpClient client = getClient();
        Call call = client.newCall(request.build());
        try (Response response = call.execute()) {
            verifySuccess(url, response);

            ResponseBody body = response.body();
            if (body == null) {
                throw new IOException("No response for " + url);
            }

            JsonParser parser = new JsonParser();
            return parser.parse(body.charStream());
        }
    }

    public static void sendPost(
            HttpUrl url,
            JsonElement body,
            Consumer<? super Request.Builder> requestConfig) throws IOException {

        String content = new Gson().toJson(body);
        sendPost(url, RequestBody.create(utf8MediaType("application/json"), content), requestConfig);
    }

    public static void sendPost(
            HttpUrl url,
            RequestBody body,
            Consumer<? super Request.Builder> requestConfig) throws IOException {
        Request.Builder request = new Request.Builder();

        request.addHeader("User-Agent", USER_AGENT);

        request.url(url);
        request.post(body);

        requestConfig.accept(request);

        OkHttpClient client = getClient();
        Call call = client.newCall(request.build());
        try (Response response = call.execute()) {
            verifySuccess(url, response);
        }
    }

    private static void verifySuccess(HttpUrl url, Response response) throws IOException {
        if (!response.isSuccessful()) {
            ResponseBody body = response.body();
            String bodyStr = body != null ? body.string() : "<NO-BODY>";
            throw new IOException("Failed http request with " + response.code() + " for " + url + "\n" + bodyStr);
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
