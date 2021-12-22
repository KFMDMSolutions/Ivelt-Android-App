package com.kfmdmsolutions.ivelt;

import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.core.text.TextUtilsCompat;
import androidx.core.view.ViewCompat;
import androidx.fragment.app.DialogFragment;
import androidx.preference.EditTextPreference;
import androidx.preference.EditTextPreferenceDialogFragmentCompat;
import androidx.preference.ListPreference;
import androidx.preference.ListPreferenceDialogFragmentCompat;
import androidx.preference.MultiSelectListPreference;
import androidx.preference.MultiSelectListPreferenceDialogFragmentCompat;
import androidx.preference.Preference;
import androidx.preference.PreferenceFragmentCompat;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.RecyclerView;

import com.kfmdmsolutions.ivelt.Utilities.Logger;

import java.util.Locale;

public class SettingsFragment extends PreferenceFragmentCompat {
    private static final String DIALOG_FRAGMENT_TAG =
            "androidx.preference.PreferenceFragment.DIALOG";
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

    @Override
    public RecyclerView onCreateRecyclerView(LayoutInflater inflater, ViewGroup parent, Bundle savedInstanceState) {
        RecyclerView recyclerView = super.onCreateRecyclerView(inflater, parent, savedInstanceState);
        recyclerView.setLayoutDirection(getLayoutDirectionFromFirstChar(Locale.getDefault()));
        return recyclerView;
    }

    private static int getLayoutDirectionFromFirstChar(@NonNull Locale locale) {
        switch(Character.getDirectionality(locale.getDisplayName(locale).charAt(0))) {
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT:
            case Character.DIRECTIONALITY_RIGHT_TO_LEFT_ARABIC:
                return ViewCompat.LAYOUT_DIRECTION_RTL;

            case Character.DIRECTIONALITY_LEFT_TO_RIGHT:
            default:
                return ViewCompat.LAYOUT_DIRECTION_LTR;
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

    @Override
    public void onDisplayPreferenceDialog(Preference preference) {

        // check if dialog is already showing
        if (getParentFragmentManager().findFragmentByTag(DIALOG_FRAGMENT_TAG) != null) {
            return;
        }

        final DialogFragment f;
        if (preference instanceof EditTextPreference) {
            f = EditTextPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else if (preference instanceof ListPreference) {
            f = RTLListPreference.newInstance(preference.getKey());
        } else if (preference instanceof MultiSelectListPreference) {
            f = MultiSelectListPreferenceDialogFragmentCompat.newInstance(preference.getKey());
        } else {
            throw new IllegalArgumentException(
                    "Cannot display dialog for an unknown Preference type: "
                            + preference.getClass().getSimpleName()
                            + ". Make sure to implement onPreferenceDisplayDialog() to handle "
                            + "displaying a custom dialog for this Preference.");
        }
        f.setTargetFragment(this, 0);
        f.show(getParentFragmentManager(), DIALOG_FRAGMENT_TAG);
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
    public static class RTLListPreference extends ListPreferenceDialogFragmentCompat {
        @NonNull
        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            Dialog d = super.onCreateDialog(savedInstanceState);
            d.getWindow().getDecorView().setLayoutDirection(getLayoutDirectionFromFirstChar(Locale.getDefault()));
            return d;
        }
        public static RTLListPreference newInstance(String key) {
            final RTLListPreference fragment =
                    new RTLListPreference();
            final Bundle b = new Bundle(1);
            b.putString(ARG_KEY, key);
            fragment.setArguments(b);
            return fragment;
        }
    }
}
