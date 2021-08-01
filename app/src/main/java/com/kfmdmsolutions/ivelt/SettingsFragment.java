package com.kfmdmsolutions.ivelt;

import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.preference.SwitchPreferenceCompat;

import com.kfmdmsolutions.ivelt.Utilities.Logger;

import java.util.Arrays;
import java.util.List;

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
//                    List<String> arrayList = Arrays.asList("Every minute", "Once in 5 minutes","Once in 10 minutes");;
//                    if(arrayList.contains(newValue.toString())){
//                        showDelayDialog(key, newValue.toString());
//                        return false;
//                    }
                    NotificationService.startNotificationService(getContext(), NotificationService.ACTION_UPDATE_DELAY_TIME);
                    return true;
                });
    }

    private void showDelayDialog(String key, String value) {
        AlertDialog.Builder builder = new AlertDialog.Builder(getContext());
        builder.setTitle("Short Refresh Time");
        builder.setMessage("Setting a short refresh time will keep your device awake and cost power and internet");
        builder.setNegativeButton(android.R.string.cancel, ((dialog, which) -> {
            dialog.dismiss();
        }));
        builder.setPositiveButton("OK", ((dialog, which) -> {
            updateDelay(key, value);
            NotificationService.startNotificationService(getContext(), NotificationService.ACTION_UPDATE_DELAY_TIME);
            dialog.dismiss();
        }));
        builder.create().show();
    }

    private void updateDelay(String key, String value) {
        Context context = getContext();
        if(context == null){
            return;
        }
        PreferenceManager.getDefaultSharedPreferences(context).edit().putString(key, value).apply();
    }
}
