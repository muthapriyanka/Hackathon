package com.example.hackathonfinal;

import android.util.Log;

import java.io.File;
import java.io.IOException;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ApiClient {

    private static final String BASE_URL = "http://yourserver.com"; // replace with your backend
    private static final OkHttpClient client = new OkHttpClient();

    public interface UploadCallback {
        void onSuccess(String response);
        void onError(Exception e);
    }

    public static void uploadFileAsync(String filePath, UploadCallback callback) {
        File file = new File(filePath);
        if (!file.exists()) {
            callback.onError(new IOException("File not found"));
            return;
        }

        RequestBody fileBody = RequestBody.create(file, MediaType.parse("audio/wav"));
        RequestBody requestBody = new MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("audio_file", file.getName(), fileBody)
                .build();

        Request request = new Request.Builder()
                .url(BASE_URL + "/api/analyze-audio")
                .post(requestBody)
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                callback.onError(e);
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (!response.isSuccessful()) {
                    callback.onError(new IOException("Unexpected code " + response));
                } else {
                    String resp = response.body() != null ? response.body().string() : "";
                    callback.onSuccess(resp);
                }
            }
        });
    }
}
