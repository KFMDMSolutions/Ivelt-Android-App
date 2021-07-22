package com.kfmdmsolutions.ivelt;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.provider.Settings;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;

import com.kfmdmsolutions.ivelt.NotificationService;

public class SettingsFragment extends PreferenceFragmentCompat {
    @Override
    public void onCreatePreferences(Bundle savedInstanceState, String rootKey) {
        setPreferencesFromResource(R.xml.settings, rootKey);
        delayPreferenceChanged("plugged_in");
        delayPreferenceChanged("pause_notifications");
        delayPreferenceChanged("battery");
        Context context = getContext();
        Preference preference = findPreference("APP_NOTIF");
        if (preference != null && context != null) {
            preference.setOnPreferenceClickListener(preference1 -> {
                Intent intent = new Intent();
                intent.setAction("android.settings.APP_NOTIFICATION_SETTINGS");
                intent.putExtra("app_package", context.getPackageName());
                intent.putExtra("app_uid", context.getApplicationInfo().uid);
                intent.putExtra("android.provider.extra.APP_PACKAGE", context.getPackageName());
                try {
                    startActivity(intent);
                }catch (Exception e){
                    android.util.Log.d("error", "can't find activity", e);
                }
                return true;
                    });
        }
        Preference emailPreference = findPreference("Logs");
        if (emailPreference != null){
            emailPreference.setOnPreferenceClickListener( emailPref -> {
                Logger.getInstance(context).emailLogs(getActivity());
                return true;
            });
        }
    }

    private void delayPreferenceChanged(String key) {
        Preference preference = findPreference(key);
        if (preference == null) return;
        preference.setOnPreferenceChangeListener(
                (changedPreference, newValue) -> {
                    NotificationService.startNotificationService(getContext(), NotificationService.ACTION_UPDATE_DELAY_TIME);
                    return true;
                });
    }

}
