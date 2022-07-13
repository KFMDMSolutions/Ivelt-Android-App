package com.android.cts.kfmdmsolutions.ivelt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
import android.text.Html;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.HashSet;

public class IveltWebInterface {
    Context context;

    public static final String IVELT_USERNAME = "com.android.cts.kfmdmsolutions.ivelt.ivelt.web.interface.ivelt.username";
    public static final String IVELT_PASSWORD = "com.android.cts.kfmdmsolutions.ivelt.ivelt.web.interface.ivelt.password";
    IveltWebInterface(Context context){
        this.context = context;
    }

    @JavascriptInterface
    public String getHiddenElements() {

        String[] hiddenArray = PreferenceManager.getDefaultSharedPreferences(context).getStringSet("hidden_buttons", new HashSet<>()).toArray(new String[]{});
        try {
            return new JSONArray(hiddenArray).toString();
        } catch (JSONException e) {
            return null;
        }
    }
    private int newposts;
    @JavascriptInterface
    public String checkForNewPostsTimes() {

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String new_posts = preferences.getString("new_posts", "Never");
        newposts = convertDurationStringToMilliSeconds(new_posts);
        if (newposts == 0){
            return null;
        }
        return ""+newposts;
    }
    @JavascriptInterface
    public boolean addsefaria(){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("sefaria", false);

    }

    @JavascriptInterface
    public void copyToClipboard(String data){
        ClipboardManager clipboard = (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("אייוועלט", data);
        clipboard.setPrimaryClip(clip);
        Toast.makeText(context, "Copied", Toast.LENGTH_SHORT).show();
    }
    @JavascriptInterface
    public void saveDefaultPage(String defaultPage){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString("default_page", defaultPage).apply();
        Toast.makeText(context,"Default Page Updated", Toast.LENGTH_SHORT).show();
    }
    @JavascriptInterface
    public boolean shouldHideUsername(){
        return PreferenceManager.getDefaultSharedPreferences(context).getBoolean("hide_profile", false);
    }

    @JavascriptInterface
    public void saveCredentials(String username, String password){
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(IVELT_USERNAME, username).putString(IVELT_PASSWORD, password).apply();
    }

    @JavascriptInterface
    public String getUsername(){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(IVELT_USERNAME, "");
//        return "בבקשה";
    }

    @JavascriptInterface
    public String getPassword(){
        return PreferenceManager.getDefaultSharedPreferences(context).getString(IVELT_PASSWORD, "");
        // TODO REMOVE BEFORE COMMIT
    }

    @JavascriptInterface
    public String getVersionString(){
        return BuildConfig.VERSION_NAME + " (" + BuildConfig.VERSION_CODE + ")";
    }





    private static final int SECOND_IN_MILLIS = 1000;
    private static final int MINUTE_IN_MILLIS = 60 * SECOND_IN_MILLIS;

    private int convertDurationStringToMilliSeconds(String duration) {
        duration = duration == null ? "" : duration;
        switch (duration) {

            case "Every 20 seconds":
                return 20 * SECOND_IN_MILLIS;
            case "Every 40 seconds":
                return 40 * SECOND_IN_MILLIS;
            case "Every minute":
                return MINUTE_IN_MILLIS;
            case "Never":
            default:
                return 0;
        }
    }
}
