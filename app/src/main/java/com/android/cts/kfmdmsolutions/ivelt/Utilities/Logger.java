package com.android.cts.kfmdmsolutions.ivelt.Utilities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import androidx.core.content.FileProvider;

import com.google.firebase.crashlytics.FirebaseCrashlytics;
import com.android.cts.kfmdmsolutions.ivelt.BuildConfig;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.lang.ref.WeakReference;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class Logger {

    WeakReference<Context> contextWeakReference;

    private static Logger instance;

    public static Logger getInstance(Context context){
        if (instance == null){
            instance = new Logger(context);
        }
        return instance;
    }

    private Logger(Context context){
        contextWeakReference = new WeakReference<>(context);
    }

    public void logWithFirebase(String entry){
        FirebaseCrashlytics.getInstance().log(entry);
        log(entry);
    }

    public void log(String entry){
        log(entry, null);
    }

    public void log(String entry, Throwable exc){

        File dir = new File(getLogFolderPath());
        dir.mkdirs();
        File logFile = lastFileModified(dir);
        boolean append = true;

        if (logFile == null || logFile.length() > 500 * 1024) {
            String dateTime = android.text.format.DateFormat.format("yyyy_MM_dd__HH_mm", new Date()).toString();
            String filePath = dir.getAbsolutePath() + File.separator + dateTime + ".log";
            logFile = new File(filePath);
            // TODO delete old log file
            deleteOldestFile(dir);
            append = false;
        }
        if (BuildConfig.DEBUG){
            android.util.Log.d("LOGGING", entry, exc);
        }
        try {
            FileWriter fileWriter = new FileWriter(logFile, append);
            SimpleDateFormat sdf = new SimpleDateFormat("MM_dd_HH:mm:ss.SSS", Locale.US);
            String time = sdf.format(new Date()) + " ";
            fileWriter.write( time + entry + "\n");
            if (exc != null){
                PrintWriter writer = new PrintWriter(fileWriter);
                exc.printStackTrace(writer);
            }
            fileWriter.close();
        } catch (IOException e) {
            FirebaseCrashlytics.getInstance().log("Unable to load logs");
        }
    }

    private String getLogFolderPath() {
        return contextWeakReference.get().getFilesDir().getAbsolutePath() + File.separator + "Logs";
    }

    public File lastFileModified(File fl) {
        File[] files = fl.listFiles(File::isFile);
        long lastMod = Long.MIN_VALUE;
        File choice = null;
        if (files == null){
            return null;
        }
        for (File file : files) {
            if (file.lastModified() > lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        return choice;
    }

    public void deleteOldestFile(File dir){
        File[] files = dir.listFiles(File::isFile);
        // Only delete oldest file if there are already 3 log files
        if (files == null || files.length < 3){
            return;
        }
        long lastMod = Long.MAX_VALUE;
        File choice = null;
        for (File file : files) {
            if (file.lastModified() < lastMod) {
                choice = file;
                lastMod = file.lastModified();
            }
        }
        if (choice != null) {
            choice.delete();
        }
    }

    public void emailLogs(Activity activity){
        File dir = new File(getLogFolderPath());
        File[] files = dir.listFiles(File::isFile);
        ArrayList<Uri> fileUris = new ArrayList<>();
        if (files == null){
            return;
        }
        try {
            for (File file : files) {
                fileUris.add(FileProvider.getUriForFile(contextWeakReference.get(), "com.android.cts.kfmdmsolutions.ivelt.fileprovider", file));
            }
        }catch (IllegalStateException ise){
            FirebaseCrashlytics.getInstance().log("Unable to email logs");
        }
        Intent intent = new Intent(Intent.ACTION_SEND_MULTIPLE);
        intent.setType("message/rfc822");
        // When sharing via email, default to my email, if you want to add other emails to copy (and\or remove mine) here is the place to do it.
        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"bevakashivelt@gmail.com","contact@kfmdmsolutions.com"});
        intent.putParcelableArrayListExtra(Intent.EXTRA_STREAM, fileUris);

        try {
            activity.startActivity(intent);
        }catch (ActivityNotFoundException activityNotFoundException){
            Logger.getInstance(activity).log("Not able to find activity", activityNotFoundException);
        }
        /*

        Intent mailIntent = new Intent(Intent.ACTION_SEND);
    mailIntent.setType("message/rfc822");
    mailIntent.putExtra(Intent.EXTRA_EMAIL, recipients);

    mailIntent.putExtra(Intent.EXTRA_SUBJECT, subject);
    mailIntent.putExtra(Intent.EXTRA_TEXT, body);
        * */
    }

}
