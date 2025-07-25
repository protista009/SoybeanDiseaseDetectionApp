package com.example.soyabean_disease;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.FrameLayout;

import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.tabs.TabLayout;
public class LegalNotices extends AppCompatActivity {

    private FrameLayout tabContent;
    private LayoutInflater inflater;
    private View legalView, privacyView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.legalnotices);

        TabLayout tabLayout = findViewById(R.id.tabLayout);
        tabContent = findViewById(R.id.tabContent);
        inflater = LayoutInflater.from(this);

        // Inflate both views once
        legalView = inflater.inflate(R.layout.tab_legal_notices, null);
        privacyView = inflater.inflate(R.layout.tab_privacy_policy, null);

        // Add tabs
        tabLayout.addTab(tabLayout.newTab().setText("Legal Notices"));
        tabLayout.addTab(tabLayout.newTab().setText("Privacy Policy"));

        // Show default tab content (Legal Notices)
        tabContent.addView(legalView);

        // Handle tab switching
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                tabContent.removeAllViews();
                if (tab.getPosition() == 0) {
                    tabContent.addView(legalView);
                } else {
                    tabContent.addView(privacyView);
                }
            }

            @Override public void onTabUnselected(TabLayout.Tab tab) {}
            @Override public void onTabReselected(TabLayout.Tab tab) {}
        });
    }
}
