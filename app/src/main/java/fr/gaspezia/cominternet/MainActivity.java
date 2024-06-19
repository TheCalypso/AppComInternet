package fr.gaspezia.cominternet;

import android.app.ProgressDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.TextView;
import android.widget.TimePicker;

import androidx.appcompat.app.AppCompatActivity;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

public class MainActivity extends AppCompatActivity {

    private DatePicker datePicker;
    private TimePicker timePicker;
    private TextView textViewResult;
    private Button buttonFetch;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initializeViews();
        buttonFetch.setOnClickListener(v -> fetchTemperature());
    }

    private void initializeViews() {
        progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Chargement...");
        progressDialog.setCancelable(false);

        datePicker = findViewById(R.id.datePicker);
        timePicker = findViewById(R.id.timePicker);
        timePicker.setIs24HourView(true);
        textViewResult = findViewById(R.id.textViewResult);
        buttonFetch = findViewById(R.id.buttonFetch);
    }

    private void fetchTemperature() {
        progressDialog.show();
        String date = String.format("%04d-%02d-%02d", datePicker.getYear(), datePicker.getMonth() + 1, datePicker.getDayOfMonth());
        String time = String.format("%02d:%02d:00", timePicker.getCurrentHour(), timePicker.getCurrentMinute());

        String urlString = "http://192.168.1.2/temperature.php?d=" + date + "&t=" + time;
        new Thread(() -> {
            try {
                URL url = new URL(urlString);
                HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
                try {
                    BufferedReader in = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                    StringBuilder response = new StringBuilder();
                    String inputLine;
                    while ((inputLine = in.readLine()) != null) {
                        response.append(inputLine);
                    }
                    in.close();
                    parseAndDisplayResult(response.toString());
                } finally {
                    urlConnection.disconnect();
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void parseAndDisplayResult(String htmlResponse) {
        String lowerCaseResponse = htmlResponse.toLowerCase();
        String result;

        if (lowerCaseResponse.contains("pas de donnée")) {
            result = "Pas de données";
        } else if (lowerCaseResponse.contains("resultat :")) {
            result = parseTemperature(lowerCaseResponse);
        } else {
            result = "Réponse inattendue";
        }

        result = result.replace("</body></html>", "").trim(); // Remove HTML tags
        String finalResult = result;
        runOnUiThread(() -> {
            textViewResult.setText(finalResult);
            progressDialog.dismiss();
        });
    }

    private String parseTemperature(String htmlResponse) {
        int startIndex = htmlResponse.indexOf("resultat :") + 10;
        int endIndex = htmlResponse.indexOf("°c", startIndex);
        return (endIndex == -1 ? htmlResponse.substring(startIndex).trim() : htmlResponse.substring(startIndex, endIndex).trim() + "°C");
    }
}