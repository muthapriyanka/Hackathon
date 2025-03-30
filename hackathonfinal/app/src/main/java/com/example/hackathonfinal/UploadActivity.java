package com.example.hackathonfinal;

import android.widget.Button;
import android.widget.TextView;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.MultipartBody;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class UploadActivity extends AppCompatActivity {
Button btnChoose, btnUpload;
TextView txtResult;
Uri audioUri;
String filePath;
final int PICK_AUDIO = 1;

// Replace with your laptop's local IP
final String SERVER_URL = "http://10.0.2.2:8000/predict";

@Override
protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_upload);

    btnChoose = findViewById(R.id.btnChooseAudio);
    btnUpload = findViewById(R.id.btnUpload);
    txtResult = findViewById(R.id.txtResult);

    btnChoose.setOnClickListener(v -> {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, PICK_AUDIO);
    });

    btnUpload.setOnClickListener(v -> {
        if (filePath != null) {
            uploadFile(filePath);
        } else {
            txtResult.setText("No audio selected.");
        }
    });
}

@Override
protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
    super.onActivityResult(requestCode, resultCode, data);

    if (requestCode == PICK_AUDIO && resultCode == RESULT_OK && data != null) {
        audioUri = data.getData();
        File file = getFileFromUri(audioUri);
        filePath = file.getAbsolutePath();
        txtResult.setText("Selected: " + file.getName());
    }
}

private File getFileFromUri(Uri uri) {
    File file = new File(getCacheDir(), "temp.wav");
    try (InputStream in = getContentResolver().openInputStream(uri);
         OutputStream out = new FileOutputStream(file)) {
        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0) {
            out.write(buf, 0, len);
        }
    } catch (Exception e) {
        e.printStackTrace();
    }
    return file;
}

private void uploadFile(String filePath) {
    OkHttpClient client = new OkHttpClient();

    File file = new File(filePath);
    RequestBody fileBody = RequestBody.create(file, MediaType.parse("audio/wav"));
    MultipartBody requestBody = new MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", file.getName(), fileBody)
            .build();

    Request request = new Request.Builder()
            .url(SERVER_URL)
            .post(requestBody)
            .build();

    client.newCall(request).enqueue(new Callback() {
        public void onFailure(Call call, IOException e) {
            runOnUiThread(() -> txtResult.setText(" Upload failed: " + e.getMessage()));
        }

        public void onResponse(Call call, Response response) throws IOException {
            if (response.isSuccessful()) {
                String result = response.body().string();
                runOnUiThread(() -> txtResult.setText(" Result: " + result));
            } else {
                runOnUiThread(() -> txtResult.setText(" Error: " + response.code()));
            }
        }
    });
}
}
