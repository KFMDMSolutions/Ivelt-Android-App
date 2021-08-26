package com.kfmdmsolutions.ivelt;

import androidx.appcompat.app.AppCompatActivity;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
//        Locale locale = new Locale("yi");
//        Locale.setDefault(locale);
//        Resources resources = getResources();
//        Configuration configuration = resources.getConfiguration();
//        configuration.setLocale(locale);
//        configuration.setLayoutDirection(locale);
//        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        findViewById(R.id.settingsDone).setOnClickListener((v) -> finish());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentView, new SettingsFragment()).commit();
    }
}
