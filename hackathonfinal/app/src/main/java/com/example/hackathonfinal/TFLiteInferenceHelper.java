package com.example.mergedapp;

import android.content.Context;

import org.tensorflow.lite.Interpreter;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.MappedByteBuffer;
import java.nio.channels.FileChannel;

public class TFLiteInferenceHelper {

    private static Interpreter tflite;

    public static float runInference(String audioFilePath, Context context) {
        if (tflite == null) {
            try {
                tflite = new Interpreter(loadModelFile(context, "my_model.tflite"));
            } catch (IOException e) {
                e.printStackTrace();
                return -1;
            }
        }

        float[][] inputBuffer = preprocessAudio(audioFilePath);
        float[][] outputBuffer = new float[1][1];

        tflite.run(inputBuffer, outputBuffer);
        return outputBuffer[0][0];
    }

    private static MappedByteBuffer loadModelFile(Context context, String modelPath) throws IOException {
        InputStream is = context.getAssets().open(modelPath);
        FileInputStream fileInputStream = (FileInputStream) is;
        FileChannel fileChannel = fileInputStream.getChannel();
        long startOffset = 0;
        long declaredLength = fileChannel.size();
        return fileChannel.map(FileChannel.MapMode.READ_ONLY, startOffset, declaredLength);
    }

    private static float[][] preprocessAudio(String audioFilePath) {
        float[][] mockInput = new float[1][128];
        for (int i = 0; i < 128; i++) {
            mockInput[0][i] = 0.0f; // dummy data
        }
        return mockInput;
    }
}
