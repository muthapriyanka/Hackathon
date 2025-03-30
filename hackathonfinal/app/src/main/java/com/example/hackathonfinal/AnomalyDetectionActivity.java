package com.example.hackathonfinal;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

public class AnomalyDetectionActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_RECORD_AUDIO = 101;
    private static final int REQUEST_CODE_SELECT_FILE = 102;

    private Button btnRecordAudio, btnSelectFile, btnUpload, btnLocalInfer;
    private TextView tvResult;

    private String selectedAudioPath;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_anomaly_detection);

        btnRecordAudio = findViewById(R.id.btnRecordAudio);
        btnSelectFile = findViewById(R.id.btnSelectFile);
        btnUpload = findViewById(R.id.btnUpload);
        btnLocalInfer = findViewById(R.id.btnLocalInfer);
        tvResult = findViewById(R.id.tvResult);

        btnRecordAudio.setOnClickListener(view -> recordAudio());
        btnSelectFile.setOnClickListener(view -> selectAudioFile());
        btnUpload.setOnClickListener(view -> {
            if (selectedAudioPath != null) {
                uploadToServerForInference(selectedAudioPath);
            } else {
                tvResult.setText("No audio file selected!");
            }
        });
        btnLocalInfer.setOnClickListener(view -> {
            if (selectedAudioPath != null) {
                runLocalInference(selectedAudioPath);
            } else {
                tvResult.setText("No audio file selected!");
            }
        });
    }

    private void recordAudio() {
        Intent intent = new Intent(MediaStore.Audio.Media.RECORD_SOUND_ACTION);
        startActivityForResult(intent, REQUEST_CODE_RECORD_AUDIO);
    }

    private void selectAudioFile() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("audio/*");
        startActivityForResult(intent, REQUEST_CODE_SELECT_FILE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode == RESULT_OK && data != null) {
            Uri audioUri = data.getData();
            selectedAudioPath = com.example.mergedapp.FilePathUtils.getPath(this, audioUri);
            tvResult.setText("Selected file: " + selectedAudioPath);
        }
    }

    private void uploadToServerForInference(String filePath) {
        ApiClient.uploadFileAsync(filePath, new ApiClient.UploadCallback() {
            @Override
            public void onSuccess(String response) {
                runOnUiThread(() -> tvResult.setText("Server says: " + response));
            }

            @Override
            public void onError(Exception e) {
                runOnUiThread(() -> tvResult.setText("Error: " + e.getMessage()));
            }
        });
    }

    private void runLocalInference(String filePath) {
        float anomalyScore = com.example.mergedapp.TFLiteInferenceHelper.runInference(filePath, this);
        tvResult.setText("Local TFLite Anomaly Score: " + anomalyScore);
    }
}
