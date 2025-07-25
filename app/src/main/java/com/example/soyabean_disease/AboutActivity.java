package com.example.soyabean_disease;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.widget.VideoView;

import androidx.appcompat.app.AppCompatActivity;

import java.util.Locale;

public class AboutActivity extends AppCompatActivity {
    @Override
    protected void attachBaseContext(Context newBase) {
        super.attachBaseContext(LocaleHelper.onAttach(newBase));
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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);


        VideoView videoView = findViewById(R.id.videoView);

        // Load video from res/raw/video_app.mp4
        Uri videoUri = Uri.parse("android.resource://" + getPackageName() + "/" + R.raw.video_app);
        videoView.setVideoURI(videoUri);

        // Start video once it's ready
        videoView.setOnPreparedListener(mp -> {
            mp.setLooping(true); // Loop the video (optional)
            videoView.start();   // Start playing
        });
    }
}
