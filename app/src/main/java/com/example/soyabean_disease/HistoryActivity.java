package com.example.soyabean_disease;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private LinearLayout historyContainer;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        historyContainer = findViewById(R.id.historyContainer);

        Button btnClear = findViewById(R.id.btnClearHistory);
        btnClear.setOnClickListener(v -> {
            new Thread(() -> {
                PredictionDatabase.getInstance(this).predictionDao().clearAll();
                runOnUiThread(() -> {
                    historyContainer.removeAllViews();
                    Toast.makeText(this, "History cleared", Toast.LENGTH_SHORT).show();
                });
            }).start();
        });

        loadHistoryFromDatabase();
    }
    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String savedLang = prefs.getString("My_Lang", "en");
        String currentLang = Locale.getDefault().getLanguage();

        if (!currentLang.equals(savedLang)) {
            LocaleHelper.setLocale(this, savedLang);
            recreate(); // only recreate if language changed
        }
    }

    private void loadHistoryFromDatabase() {
        new Thread(() -> {
            List<PredictionEntry> entries = PredictionDatabase.getInstance(this)
                    .predictionDao()
                    .getAllPredictions();

            runOnUiThread(() -> {
                historyContainer.removeAllViews();
                for (PredictionEntry entry : entries) {
                    addHistoryCard(historyContainer, entry);
                }
            });
        }).start();
    }

    private void addHistoryCard(LinearLayout container, PredictionEntry entry) {
        LayoutInflater inflater = LayoutInflater.from(this);
        View card = inflater.inflate(R.layout.item_history, null);

        ImageView imageView = card.findViewById(R.id.historyImage);
        TextView resultView = card.findViewById(R.id.historyResult);
        TextView confidenceView = card.findViewById(R.id.historyConfidence);

        Bitmap bitmap = BitmapFactory.decodeFile(entry.imagePath);
        if (bitmap != null) {
            imageView.setImageBitmap(bitmap);
        } else {
            imageView.setImageResource(R.drawable.placeholder_leaf);
        }

        resultView.setText(getString(R.string.result) + "\n" + entry.result);


        String formattedTime = new SimpleDateFormat("dd MMM yyyy, hh:mm a", Locale.getDefault())
                .format(new Date(entry.timestamp));

        confidenceView.setText(String.format(Locale.US,
                getString(R.string.confidence_and_time), entry.confidence * 100, formattedTime));


        container.addView(card);
    }
}
