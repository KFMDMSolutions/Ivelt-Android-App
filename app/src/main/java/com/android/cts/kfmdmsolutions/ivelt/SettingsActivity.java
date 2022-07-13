package com.android.cts.kfmdmsolutions.ivelt;

import android.content.res.Configuration;
import android.content.res.Resources;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;
import androidx.preference.PreferenceManager;

import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        String language = PreferenceManager.getDefaultSharedPreferences(this).getString("language", "en");
        Locale locale = new Locale(language);
        Locale.setDefault(locale);
        Resources resources = getResources();
        Configuration configuration = resources.getConfiguration();
        configuration.setLocale(locale);
        android.util.Log.d("LOCOLOC", "YI locale direction " + TextUtils.getLayoutDirectionFromLocale(new Locale("ji")));
        getWindow().getDecorView().setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
        resources.updateConfiguration(configuration, resources.getDisplayMetrics());

        findViewById(R.id.settingsDone).setOnClickListener((v) -> finish());
        getSupportFragmentManager().beginTransaction().replace(R.id.fragmentView, new SettingsFragment()).commit();
    }
}
