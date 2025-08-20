package com.example.soyabean_disease;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;

    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recyclerViewHistory);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new HistoryAdapter(this);
        recyclerView.setAdapter(adapter);

        Button btnClear = findViewById(R.id.btnClearHistory);
        btnClear.setOnClickListener(v -> {
            new Thread(() -> {
                PredictionDatabase.getInstance(this).predictionDao().clearAll();
                runOnUiThread(() -> {
                    adapter.setData(null);
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
            recreate();
        }
    }

    private void loadHistoryFromDatabase() {
        new Thread(() -> {
            List<PredictionEntry> entries = PredictionDatabase.getInstance(this)
                    .predictionDao()
                    .getAllPredictions();

            runOnUiThread(() -> adapter.setData(entries));
        }).start();
    }
}
