package com.kfmdmsolutions.ivelt;

import android.content.Context;
import android.content.SharedPreferences;
import android.webkit.JavascriptInterface;

import androidx.preference.PreferenceManager;

public class IveltWebInterface {
    Context context;

    public static final String IVELT_USERNAME = "com.kfmdmsolutions.ivelt.ivelt.web.interface.ivelt.username";
    public static final String IVELT_PASSWORD = "com.kfmdmsolutions.ivelt.ivelt.web.interface.ivelt.password";
    IveltWebInterface(Context context){
        this.context = context;
    }

    @JavascriptInterface
    public void saveCredentials(String username, String password){
        android.util.Log.d("IWI", "in android username is " + username + " password is " + password);
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
}
