package com.kfmdmsolutions.ivelt;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.text.Html;
import android.webkit.JavascriptInterface;
import android.widget.Toast;

import androidx.preference.PreferenceManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import com.kfmdmsolutions.ivelt.Utilities.Logger;
import java.util.HashSet;

public class IveltWebInterface {
    Context context;

    public static final String IVELT_USERNAME = "com.kfmdmsolutions.ivelt.ivelt.web.interface.ivelt.username";
    public static final String IVELT_PASSWORD = "com.kfmdmsolutions.ivelt.ivelt.web.interface.ivelt.password";
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

    @JavascriptInterface
    public void sharePost(String html){
        String parsedHtml = parsePostHtml(html);
        Intent shareIntent = new Intent(android.content.Intent.ACTION_SEND);
        CharSequence text = Html.fromHtml(html, Html.FROM_HTML_MODE_COMPACT);
        shareIntent.putExtra(Intent.EXTRA_HTML_TEXT, html);
        shareIntent.putExtra(Intent.EXTRA_TEXT, parsedHtml);
        shareIntent.putExtra(Intent.EXTRA_TITLE, "Share");
        shareIntent.setType("text/*");
//        context.startActivity(shareIntent);
        context.startActivity(Intent.createChooser(shareIntent, "null"));
    }

    private String parsePostHtml(String html){
        Document doc = Jsoup.parse(html);
        Element body = doc.body();
        body.select("img").forEach(element -> {
            String src = element.attr("src");
            if (src.startsWith("./")){
                src = src.replace("./", "www.ivelt.com/forum/");
            }
            element.text("<br>" + src + "<br>");
        });
        body.select("a").forEach(element -> {
            if (!element.text().matches("^(https:|http:|www\\. |(./)?viewtopic)\\S*")) {
                element.text("<br>" + element.text() + " " + element.attr("href") + "<br>");
            }
        });
        body.select(".file dd").forEach(element -> element.text(""));
        body.select("cite").forEach(element -> element.text(""));
        body.select("blockquote").forEach( element -> {
            String text = element.text();
            element.text("[quote]<br> " + text + " <br>[end quote]<br>");
        });

        return Html.fromHtml(body.html(), Html.FROM_HTML_MODE_COMPACT)
                .toString()
                .replaceAll("<br>", "\n")
                .replaceAll("(?:^|\\s)viewtopic", "www.ivelt.com/forum/viewtopic")
                .replaceAll("\n+", "\n")
                ;
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
