package com.kfmdmsolutions.ivelt;

import android.os.Bundle;

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
    }

    private void delayPreferenceChanged(String key) {
        Preference preference = findPreference(key);
        if (preference == null) return;
        preference.setOnPreferenceChangeListener(
                (changedPreference, newValue) -> {
                   NotificationService.startNotificationService(getContext(), NotificationService.ACTION_UPDATE_DELAY_TIME);
                    return true;
                }
                );
    }

}
