package com.example.hackathonfinal;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import org.json.JSONObject;

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

public class MainActivity extends AppCompatActivity {

    private Button btnEsg, btnAnomaly, btnGemini;
    private static final int PICK_AUDIO_REQUEST = 1;
    private Uri audioUri;
    private File audioFile;

    Button btnCallML, btnApi;
   ImageButton btnChatGPT, btnCarML;
    ImageView iconWave;
    TextView txtResult;

    final String SERVER_URL = "http://10.0.2.2:8000/predict";
    @SuppressLint({"WrongViewCast", "MissingInflatedId"})
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnEsg = findViewById(R.id.btnEsg);
//        btnAnomaly = findViewById(R.id.btnAnomaly);
        btnChatGPT = findViewById(R.id.btnChatGPT);

            // 🔧 UI Components
            btnCallML = findViewById(R.id.btnCallML);
            iconWave = findViewById(R.id.iconWave);
            txtResult = findViewById(R.id.txtResult);

            // 🎵 Select audio
            iconWave.setOnClickListener(v -> selectAudioFile());

            // 🧠 ML Prediction
            btnCallML.setOnClickListener(v -> {
                if (audioFile != null) {
                    uploadAudioFile(audioFile);
                } else {
                    Toast.makeText(this, "Please select an audio file first", Toast.LENGTH_SHORT).show();
                }
            });

            // 🚗 Upload screen
//            btnCarML.setOnClickListener(v -> {
//                Intent intent = new Intent(MainActivity.this, UploadActivity.class);
//                startActivity(intent);
//            });
        btnEsg.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, EsgActivity.class)));

//        btnAnomaly.setOnClickListener(view ->
//                startActivity(new Intent(MainActivity.this, AnomalyDetectionActivity.class)));

        btnChatGPT.setOnClickListener(view ->
                startActivity(new Intent(MainActivity.this, GeminiActivity.class)));
    }
//        btnGemini.setOnClickListener(view ->
//                startActivity(new Intent(MainActivity.this, GeminiActivity.class)));
//        }

        private void selectAudioFile() {
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.setType("audio/wav");
            startActivityForResult(intent, PICK_AUDIO_REQUEST);
        }

        @Override
        protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            super.onActivityResult(requestCode, resultCode, data);
            if (requestCode == PICK_AUDIO_REQUEST && resultCode == Activity.RESULT_OK && data != null) {
                audioUri = data.getData();
                audioFile = createTempFileFromUri(audioUri);
                Toast.makeText(this, "Audio selected", Toast.LENGTH_SHORT).show();
            }
        }

        private File createTempFileFromUri(Uri uri) {
            try {
                InputStream inputStream = getContentResolver().openInputStream(uri);
                String fileName = getFileName(uri);
                File tempFile = new File(getCacheDir(), fileName);
                try (OutputStream outputStream = new FileOutputStream(tempFile)) {
                    byte[] buffer = new byte[4096];
                    int bytesRead;
                    while ((bytesRead = inputStream.read(buffer)) != -1) {
                        outputStream.write(buffer, 0, bytesRead);
                    }
                    return tempFile;
                }
            } catch (IOException e) {
                e.printStackTrace();
                Toast.makeText(this, "Failed to load audio file", Toast.LENGTH_SHORT).show();
                return null;
            }
        }

        private String getFileName(Uri uri) {
            String result = null;
            if ("content".equals(uri.getScheme())) {
                try (Cursor cursor = getContentResolver().query(uri, null, null, null, null)) {
                    if (cursor != null && cursor.moveToFirst()) {
                        int index = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
                        if (index >= 0) {
                            result = cursor.getString(index);
                        }
                    }
                }
            }
            if (result == null) {
                result = uri.getLastPathSegment();
            }
            return result;
        }

        private void uploadAudioFile(File file) {
            OkHttpClient client = new OkHttpClient();
            RequestBody fileBody = RequestBody.create(file, MediaType.parse("audio/wav"));
            RequestBody requestBody = new MultipartBody.Builder()
                    .setType(MultipartBody.FORM)
                    .addFormDataPart("file", file.getName(), fileBody)
                    .build();

            Request request = new Request.Builder()
                    .url(SERVER_URL)
                    .post(requestBody)
                    .build();

            client.newCall(request).enqueue(new Callback() {
                @Override
                public void onFailure(Call call, IOException e) {
                    runOnUiThread(() -> txtResult.setText("❌ Upload failed: " + e.getMessage()));
                }

                @Override
                public void onResponse(Call call, Response response) throws IOException {
                    if (response.isSuccessful()) {
                        String result = response.body().string();
                        runOnUiThread(() -> {
                            try {
                                JSONObject json = new JSONObject(result);
                                String predictedClass = json.getString("prediction");
                                double confidence = json.getDouble("confidence");

                                String formatted = "Predicted Class: " + predictedClass.toUpperCase() +
                                        "\nConfidence = " + String.format("%.5f", confidence);
                                txtResult.setText(formatted);
                            } catch (Exception ex) {
                                txtResult.setText("❌ Failed to parse response.");
                                ex.printStackTrace();
                            }
                        });
                    } else {
                        runOnUiThread(() -> txtResult.setText("⚠️ Server error: " + response.code()));
                    }
                }
            });


    }
}
