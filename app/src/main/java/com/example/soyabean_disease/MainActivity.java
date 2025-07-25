package com.example.soyabean_disease;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.transition.TransitionManager;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.cardview.widget.CardView;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicReference;

public class MainActivity extends AppCompatActivity {



    private Button btnAbout;
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
    }

    @SuppressLint("MissingInflatedId")

    private void loadLocale() {
        SharedPreferences prefs = getSharedPreferences("settings", MODE_PRIVATE);
        String savedLang = prefs.getString("My_Lang", "en");
        String currentLang = Locale.getDefault().getLanguage();

        if (!currentLang.equals(savedLang)) {
            LocaleHelper.setLocale(this, savedLang);
            recreate(); // only recreate if language changed
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);




        int[] cardIds = {
                R.id.card_sudden_death,
                R.id.card_septoria,
                R.id.card_southern_blight,
                R.id.card_yellow_mossaic,
                R.id.card_mossaic_virus,
                R.id.card_powdery_mildew,
                R.id.card_ferrugen,
                R.id.card_bacterial_blight,
                R.id.card_brown_spot
        };


        AtomicReference<TextView> currentlyVisibleDesc = new AtomicReference<>();

        for (int id : cardIds) {
            CardView card = findViewById(id);
            String descIdStr = (String) card.getTag();

            int descId = getResources().getIdentifier(descIdStr, "id", getPackageName());
            TextView descView = findViewById(descId);

            card.setOnClickListener(v -> {
                TransitionManager.beginDelayedTransition(card);

                if (descView.getVisibility() == View.VISIBLE) {
                    descView.setVisibility(View.GONE);
                    currentlyVisibleDesc.set(null);
                } else {
                    // Hide previously visible description
                    if (currentlyVisibleDesc.get() != null) {
                        currentlyVisibleDesc.get().setVisibility(View.GONE);
                    }
                    // Show this one
                    descView.setVisibility(View.VISIBLE);
                    currentlyVisibleDesc.set(descView);
                }
            });
        }



       
    }
}
