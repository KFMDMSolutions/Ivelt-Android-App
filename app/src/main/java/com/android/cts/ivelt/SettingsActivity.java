package com.android.cts.ivelt;

import androidx.appcompat.app.AppCompatActivity;

import android.os.Bundle;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        findViewById(R.id.settingsDone).setOnClickListener((v) -> finish());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentView, new SettingsFragment()).commit();
    }
}