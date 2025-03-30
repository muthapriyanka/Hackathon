package com.example.hackathonfinal;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class EsgActivity extends AppCompatActivity {

    private TextView gdpTextView, agriTextView, co2TextView;
    private LineChart chartGDP, chartAgri, chartCO2;

    private final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_esg);

        chartGDP = findViewById(R.id.lineChartGDP);
        chartAgri = findViewById(R.id.lineChartAgri);
        chartCO2 = findViewById(R.id.lineChartCO2);

        gdpTextView = findViewById(R.id.textViewGDP);
        agriTextView = findViewById(R.id.textViewAgri);
        co2TextView = findViewById(R.id.textViewCO2);

        fetchGDPGrowthRate();
        fetchAgriculturalLand();
        fetchCO2Emissions();
    }

    private void populateChart(LineChart chart, ArrayList<Entry> entries, String label) {
        LineDataSet dataSet = new LineDataSet(entries, label);
        dataSet.setLineWidth(2f);
        dataSet.setValueTextSize(10f);
        dataSet.setColor(Color.BLUE);
        dataSet.setCircleColor(Color.RED);
        dataSet.setCircleRadius(4f);

        LineData lineData = new LineData(dataSet);
        chart.setData(lineData);
        chart.getXAxis().setPosition(XAxis.XAxisPosition.BOTTOM);
        chart.getDescription().setEnabled(false);
        chart.invalidate();
    }

    private void fetchGDPGrowthRate() {
        String url = "https://api.worldbank.org/v2/country/WLD/indicator/NY.GDP.MKTP.KD.ZG?format=json";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("EsgActivity", "❌ GDP request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray root = new JSONArray(response.body().string());
                    JSONArray data = root.getJSONArray(1);
                    ArrayList<Entry> gdpEntries = new ArrayList<>();
                    for (int i = 0; i < data.length(); i++) {
                        JSONObject entry = data.getJSONObject(i);
                        if (!entry.isNull("value")) {
                            float value = (float) entry.getDouble("value");
                            int year = Integer.parseInt(entry.getString("date"));
                            gdpEntries.add(new Entry(year, value));
                        }
                    }
                    Collections.sort(gdpEntries, Comparator.comparing(Entry::getX));
                    JSONObject latest = data.getJSONObject(0);
                    double gdp = latest.getDouble("value");
                    String year = latest.getString("date");
                    runOnUiThread(() -> {
                        gdpTextView.setText("GDP Growth Rate: " + gdp + " (" + year + ")");
                        populateChart(chartGDP, gdpEntries, "GDP Growth Rate (%)");
                    });
                } catch (JSONException e) {
                    Log.e("EsgActivity", "❌ GDP JSON Parsing failed: " + e.getMessage());
                }
            }
        });
    }

    private void fetchAgriculturalLand() {
        String url = "https://api.worldbank.org/v2/country/WLD/indicator/AG.LND.AGRI.ZS?format=json";
        Request request = new Request.Builder().url(url).build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("EsgActivity", "❌ Agri request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONArray root = new JSONArray(response.body().string());
                    JSONArray data = root.getJSONArray(1);
                    ArrayList<Entry> agriEntries = new ArrayList<>();
                    String latestValue = "N/A";
                    String latestYear = "N/A";

                    for (int i = 0; i < data.length(); i++) {
                        JSONObject entry = data.getJSONObject(i);
                        if (!entry.isNull("value")) {
                            float value = (float) entry.getDouble("value");
                            int year = Integer.parseInt(entry.getString("date"));
                            agriEntries.add(new Entry(year, value));

                            if (latestValue.equals("N/A")) {
                                latestValue = String.valueOf(value);
                                latestYear = entry.getString("date");
                            }
                        }
                    }

                    Collections.sort(agriEntries, Comparator.comparing(Entry::getX));
                    String finalValue = latestValue;
                    String finalYear = latestYear;

                    runOnUiThread(() -> {
                        agriTextView.setText("Agricultural Land (%): " + finalValue + " (" + finalYear + ")");
                        populateChart(chartAgri, agriEntries, "Agricultural Land (%)");
                    });
                } catch (JSONException e) {
                    Log.e("EsgActivity", "❌ Agri JSON Parsing failed: " + e.getMessage());
                    runOnUiThread(() -> agriTextView.setText("Agricultural Land (%): Error"));
                }
            }
        });
    }

    private void fetchCO2Emissions() {
        String url = "https://www.climatewatchdata.org/api/v1/data/historical_emissions?country_iso=WLD&source=EDGAR&sector=Total%20excluding%20LUCF&gas=CO2";

        Request request = new Request.Builder()
                .url(url)
                .addHeader("Accept", "application/json")
                .build();

        client.newCall(request).enqueue(new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
                Log.e("EsgActivity", "❌ CO2 request failed: " + e.getMessage());
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                try {
                    JSONObject root = new JSONObject(response.body().string());
                    JSONArray dataArray = root.getJSONArray("data");
                    ArrayList<Entry> co2Entries = new ArrayList<>();
                    if (dataArray.length() > 0) {
                        JSONObject entry = dataArray.getJSONObject(0);
                        JSONArray emissions = entry.getJSONArray("emissions");
                        for (int i = 0; i < emissions.length(); i++) {
                            JSONObject record = emissions.getJSONObject(i);
                            if (!record.isNull("value")) {
                                double co2_kt = record.getDouble("value");
                                int year = Integer.parseInt(record.getString("year"));
                                co2Entries.add(new Entry(year, (float) co2_kt * 1000));
                            }
                        }
                        Collections.sort(co2Entries, Comparator.comparing(Entry::getX));
                        JSONObject latest = emissions.getJSONObject(emissions.length() - 1);
                        double co2_kt = latest.getDouble("value");
                        String year = latest.getString("year");
                        double co2_tonnes = co2_kt * 1000;
                        String formatted = String.format(Locale.US, "%,.0f", co2_tonnes);
                        String displayText = "CO₂ Emissions: " + formatted + " t (" + year + ")";
                        runOnUiThread(() -> {
                            co2TextView.setText(displayText);
                            populateChart(chartCO2, co2Entries, "CO₂ Emissions (t)");
                        });
                    } else {
                        runOnUiThread(() -> co2TextView.setText("CO₂ Emissions: N/A"));
                    }
                } catch (JSONException e) {
                    Log.e("EsgActivity", "❌ CO2 JSON Parsing failed: " + e.getMessage());
                    runOnUiThread(() -> co2TextView.setText("CO₂ Emissions: Error"));
                }
            }
        });
    }
}
