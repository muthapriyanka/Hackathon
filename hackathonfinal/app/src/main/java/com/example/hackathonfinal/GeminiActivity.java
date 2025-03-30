package com.example.hackathonfinal;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class GeminiActivity extends AppCompatActivity {

    private EditText userInput;
    private ScrollView scrollView;
    private LinearLayout messageContainer;
    private LineChart lineChart;

    private static final String API_KEY = "AIzaSyAHZtKt4qFEwo6UonXvc-KRxFZ9V2iw_kM";
    private static final String API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=" + API_KEY;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_gemini);

        userInput = findViewById(R.id.user_input);
        scrollView = findViewById(R.id.scrollView);
        messageContainer = findViewById(R.id.message_container);
        lineChart = findViewById(R.id.line_chart);
        ImageButton sendBtn = findViewById(R.id.send_button);

        sendBtn.setOnClickListener(v -> {
            String prompt = userInput.getText().toString().trim();
            if (!prompt.isEmpty()) {
                addMessage(prompt, true);
                sendPromptToGemini(prompt);
                userInput.setText("");
            }
        });
    }

    private void addMessage(String text, boolean isUser) {
        View messageView = getLayoutInflater().inflate(R.layout.message_item, null);
        TextView messageText = messageView.findViewById(R.id.message_text);
        ImageView icon = messageView.findViewById(R.id.icon);

        messageText.setText(text);
        icon.setImageResource(isUser ? R.drawable.ic_user : R.drawable.ic_gemini);

        messageContainer.addView(messageView);
        scrollView.post(() -> scrollView.fullScroll(View.FOCUS_DOWN));
    }

    private void sendPromptToGemini(String prompt) {
        OkHttpClient client = new OkHttpClient();
        JSONObject json = new JSONObject();

        try {
            JSONArray contents = new JSONArray();
            JSONObject content = new JSONObject();
            JSONArray parts = new JSONArray();
            JSONObject part = new JSONObject();
            part.put("text", prompt);
            parts.put(part);
            content.put("parts", parts);
            contents.put(content);
            json.put("contents", contents);
        } catch (Exception e) {
            e.printStackTrace();
        }

        RequestBody body = RequestBody.create(json.toString(), MediaType.get("application/json"));
        Request request = new Request.Builder().url(API_URL).post(body).build();

        client.newCall(request).enqueue(new Callback() {
            public void onFailure(Call call, IOException e) {
                runOnUiThread(() -> addMessage("❌ Error: " + e.getMessage(), false));
            }

            public void onResponse(Call call, Response response) throws IOException {
                if (response.isSuccessful()) {
                    String res = response.body().string();
                    try {
                        JSONObject jsonResponse = new JSONObject(res);
                        JSONArray candidates = jsonResponse.getJSONArray("candidates");
                        JSONObject first = candidates.getJSONObject(0);
                        JSONObject content = first.getJSONObject("content");
                        JSONArray parts = content.getJSONArray("parts");
                        String reply = parts.getJSONObject(0).getString("text");

                        runOnUiThread(() -> {
                            String cleaned = reply
                                    .replaceAll("(?s)```json", "")
                                    .replaceAll("(?s)```", "")
                                    .replaceAll("(?i)note:.*", "")
                                    .trim();

                            if (cleaned.startsWith("[") && cleaned.endsWith("]")) {
                                parseAndDisplayGraph(cleaned);
                            } else {
                                addMessage("❗ Gemini did not return a valid JSON array.", false);
                            }
                        });

                    } catch (Exception e) {
                        runOnUiThread(() -> addMessage("❗ Parsing error: " + e.getMessage(), false));
                    }
                } else {
                    runOnUiThread(() -> addMessage("⚠️ API Error: " + response.message(), false));
                }
            }
        });
    }

    private void parseAndDisplayGraph(String jsonText) {
        try {
            JSONArray dataArray = new JSONArray(jsonText);
            ArrayList<Entry> entries = new ArrayList<>();

            for (int i = 0; i < dataArray.length(); i++) {
                JSONObject item = dataArray.getJSONObject(i);
                int year = item.getInt("year");
                float value = (float) item.getDouble("value");
                entries.add(new Entry(year, value));
            }

            lineChart.clear();
            LineDataSet dataSet = new LineDataSet(entries, "Gemini Data");
            lineChart.setData(new LineData(dataSet));
            lineChart.getDescription().setText("Year vs Value");
            findViewById(R.id.chart_container).setVisibility(View.VISIBLE);
            lineChart.invalidate();

        } catch (Exception e) {
            addMessage("📉 Graph error: " + e.getMessage(), false);
        }
    }
}
