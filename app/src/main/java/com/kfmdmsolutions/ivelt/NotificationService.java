package com.kfmdmsolutions.ivelt;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.Context;
import android.content.IntentFilter;
import android.content.SharedPreferences;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.TimeUnit;

import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.IBinder;
import android.text.Html;
import android.text.format.DateFormat;
import android.text.format.DateUtils;
import android.webkit.CookieManager;

import androidx.annotation.Nullable;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.preference.PreferenceManager;
import androidx.work.Constraints;
import androidx.work.ExistingPeriodicWorkPolicy;
import androidx.work.NetworkType;
import androidx.work.PeriodicWorkRequest;
import androidx.work.WorkManager;

import com.kfmdmsolutions.ivelt.Utilities.Logger;
import com.kfmdmsolutions.ivelt.Utilities.Utils;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static com.kfmdmsolutions.ivelt.MainActivity.coreCookieManager;
import static java.text.DateFormat.DEFAULT;
import static java.text.DateFormat.MEDIUM;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * helper methods.
 */
public class NotificationService extends Service {

    public static final String DIRECT_MESSAGES_NOTIFICATION_CHANNEL = "DM";
    public static final String QUOTES_NOTIFICATION_CHANNEL = "Quotes";
    public static final String BOOKMARK_NOTIFICATION_CHANNEL = "BM";
    public static final String OTHER_NOTIFICATION_CHANNEL = "other";
    public static final String SERVICE_NOTIFICATION_CHANNEL = "service";
    public static final String SENT_NOTIFICATION_LIST = "SentNotificationList";
    public static NotificationService instance;

    public static String ACTION_UPDATE_DELAY_TIME = "com.android.cts.ivelt.notification.service.action.update.delay.time";
    public static String ACTION_CHECK_NOTIFICATIONS = "com.android.cts.ivelt.notification.service.action.check.notifications";
    private int pluggedInDelay;
    private int batteryDelay;

    private static ConcurrentLinkedQueue<Integer> sentNotificationQueue;
    public static NotificationService getInstance(){
        if(instance == null){
            instance = new NotificationService();
        }
        return instance;
    }

    public static void initChannels(Context context){
        createNotificationChannel(context, "Direct Messages", "Show a notification when you get a direct message", DIRECT_MESSAGES_NOTIFICATION_CHANNEL);
        createNotificationChannel( context, "Quotes", "Show a notification when someone quotes you", QUOTES_NOTIFICATION_CHANNEL);
        createNotificationChannel(context, "Bookmarks", "Show a notification when there is a new post in a thread you bookmarked", BOOKMARK_NOTIFICATION_CHANNEL);
        createNotificationChannel(context, "Other Notifications", "Show other notifications, e. g. Report closed, Notification Accepted etc.", OTHER_NOTIFICATION_CHANNEL);
        createNotificationChannel(context, "Service Notifications", "Show app service notifications - needed for updating the notification", SERVICE_NOTIFICATION_CHANNEL);
    }
    private static void createNotificationChannel(Context context, String name, String description, String channelID) {
        // Create the NotificationChannel, but only on API 26+ because
        // the NotificationChannel class is new and not in the support library
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//            CharSequence name = getString(R.string.channel_name);
//            String description = getString(R.string.channel_description);
            int importance = name.equals(SERVICE_NOTIFICATION_CHANNEL) ? NotificationManager.IMPORTANCE_MIN : NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            if (context != null){
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void startNotificationService(Context context, String action){
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(action);
        context.startService(intent);
    }
    public static void checkNotifications(Context context){
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(ACTION_CHECK_NOTIFICATIONS);
        context.startService(intent);
    }

    public static void checkForNotifications (Context context){

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();
        if (!connected){
            Logger.getInstance(context).log("Not checking for updates, not connected to internet");
            return;
        }
        String url = "http://www.ivelt.com/forum/ucp.php?i=ucp_notifications";
        Utils.executeAsync(() -> {
            try {
                Document doc = Utils.getConnection(url, null, context).get();
                Elements notificationList = doc.select(".notification_list");
                Elements newNotifications = notificationList.select(".row.bg3");
                Elements notifications = newNotifications.select(".notifications");
                for (Element element: notifications){
                    android.util.Log.d("Notif", "Element " + element.child(0).attr("href"));
                    NotificationInfo info = new NotificationInfo(element);
                    showNotification(context, info);
                }
                if (notifications.isEmpty()){
                    Logger.getInstance(context).log("Do we have notifications? " + (notificationList.first() != null));
                }
                Logger.getInstance(context).log("Notifications found: " + notifications.size());
            } catch (IOException e) {
                Logger.getInstance(context).log("Checking for notifications failed", e);
                e.printStackTrace();
            }
        });
    }

    private static void showNotification(Context context, NotificationInfo notificationInfo){

        if (sentNotificationQueue.contains(notificationInfo.id)){
            Logger.getInstance(context).log("Notification with ID " + notificationInfo.id + " already shown");
            return;
        }
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        intent.putExtra(MainActivity.EXTRA_URL, notificationInfo.url);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context.getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(intent);
        PendingIntent pendingIntent = stackBuilder.getPendingIntent(notificationInfo.id, PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context.getApplicationContext(), notificationInfo.channelID)
                .setSmallIcon(R.drawable.ivelt_logo48)
                .setContentTitle(notificationInfo.title)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setContentText(notificationInfo.text)
                .setPriority(NotificationCompat.PRIORITY_MAX);
        NotificationManagerCompat.from(context.getApplicationContext()).notify(notificationInfo.id, notificationBuilder.build());
        sentNotificationQueue.add(notificationInfo.id);
        if(sentNotificationQueue.size() > 50){
            sentNotificationQueue.remove();
        }
        Logger.getInstance(context).log("Sent notification with ID " + notificationInfo.id);
    }
    enum NotificationType {
        PRIVATE_MESSAGE(DIRECT_MESSAGES_NOTIFICATION_CHANNEL), QUOTE(QUOTES_NOTIFICATION_CHANNEL), BOOKMARK(BOOKMARK_NOTIFICATION_CHANNEL), OTHER(OTHER_NOTIFICATION_CHANNEL);

        String channelID;

        NotificationType(String channelID){
            this.channelID = channelID;
        }

        static NotificationType findType(String name){
            if (name == null){
                return OTHER;
            }
            switch (name.trim()){
                case "פריוואטע מעסעדזש":
                    return PRIVATE_MESSAGE;
                case "ציטירט":
                    return QUOTE;
                case "תגובה":
                case "נייע אשכול":
                    return BOOKMARK;
                default:
                    return OTHER;
            }
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        try {

            FileInputStream fis = openFileInput(SENT_NOTIFICATION_LIST);
            ObjectInputStream is = new ObjectInputStream(fis);
            sentNotificationQueue = (ConcurrentLinkedQueue<Integer>) is.readObject();
            is.close();
            fis.close();
        } catch (IOException | ClassNotFoundException | ClassCastException e) {
            sentNotificationQueue = new ConcurrentLinkedQueue<>();
            Logger.getInstance(this).log("unable to deserialize sent notification queue, created a new one");
            e.printStackTrace();
        }
    }

    public static class NotificationInfo{
        NotificationType type;
        CharSequence title;
        String channelID;
        CharSequence text;
        String url;
        int id;

        NotificationInfo(Element notificationElement){

            Elements link = notificationElement.getElementsByTag("a");
            if (link.first() != null){
                String url = link.first().attr("href");
                android.util.Log.d("Notif", url);
                this.id = extractIDFromURL(url);
                this.url = fixURL(url);
                android.util.Log.d("Notif", "url is " + this.url );
            }else{
                android.util.Log.d("Notif", "url null");
            }
            Elements title = notificationElement.getElementsByClass("notifications_title");
            if (title.first() != null){
                Elements strong = title.first().getElementsByTag("strong");
                if (strong.first() != null){
                    this.title = Html.fromHtml(strong.html(), 0).toString();
                    this.type = NotificationType.findType(this.title.toString());
                    this.channelID = this.type.channelID;
                    android.util.Log.d("Notif", "title " + this.title + " type " + this.type);
                }
                this.text = Html.fromHtml(title.first().html(),0);
            }
        }

        private String fixURL(String url){
            return url.replace("./", "http://www.ivelt.com/forum/");
        }

        public static int extractIDFromURL(String url){
            Pattern pattern = Pattern.compile("mark_notification=([^;]*)&");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find())
            {
                int id = Integer.parseInt(matcher.group().replaceAll("[^\\d]", ""));
                android.util.Log.d("Notif", "id is " + id);
                return id;
            }
            return -1;
        }
    }

    private boolean shouldStartForeground(boolean paused, int pluggedInDelay, int batteryDelay){

        // using plus , will be 0 if both are 0, if only one is 0, it will be the other number, and if both are non 0, it will be some unknown number
        int delayTime = pluggedInDelay + batteryDelay;

        // both are 0 return false, or when notifications are paused.
        if (paused || ((delayTime) == 0)){
            return false;
        }
        // Both are non 0, delay time is some unknown number, we need to set it the minimum delay time.
        if ((pluggedInDelay * batteryDelay) > 0){
            delayTime = Math.min(pluggedInDelay, batteryDelay);
        }
        return ((delayTime < 15 * MINUTE_IN_MILLIS));
    }


    @Override
    public void onDestroy() {
        try {
            FileOutputStream fos = openFileOutput(SENT_NOTIFICATION_LIST, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(sentNotificationQueue);
            os.close();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        super.onDestroy();
    }

    private static final int MINUTE_IN_MILLIS = 60 * 1000;
    private static final int HOUR_IN_MILLIS = 60 * MINUTE_IN_MILLIS;
    private int convertDurationStringToMilliSeconds(String duration){
        duration = duration == null ? "" : duration;
        switch (duration){
            case "Every minute":
                return MINUTE_IN_MILLIS;
            case "Once in 5 minutes":
                return 5 * MINUTE_IN_MILLIS;
            case "Once in 10 minutes":
                return 10 * MINUTE_IN_MILLIS;
            case "Once in 15 minutes":
                return 15 * MINUTE_IN_MILLIS;
            case "Once every half hour":
                return 30 * MINUTE_IN_MILLIS;
            case "Once every hour":
                return HOUR_IN_MILLIS;
            case "Once in 2 hours":
                return 2 * HOUR_IN_MILLIS;
            case "Once in 3 hours":
                return 3 * HOUR_IN_MILLIS;
            case "Once in 6 hours":
                return 6 * HOUR_IN_MILLIS;
            case "Once in 12 hours":
                return 12 * HOUR_IN_MILLIS;
            case "Once a day":
                return 24 * HOUR_IN_MILLIS;
            case "Never":
            default:
                return 0;
        }
    }
    public NotificationService() {
        super();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }


    private static final int SERVICE_NOTIFICATION_ID = 1000;
    private void startForeground(){
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        Notification notification = updateServiceNotification();
        startForeground(SERVICE_NOTIFICATION_ID, notification);
        registerReceiver(powerBroadcastReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
        lastUpdatedTime = 0;
        isCharging = batteryManager.isCharging();
        startTimer();
    }


    TimerTask task;
    private void startTimer() {
        long period = getPeriod();
//        android.util.Log.d("StartForeground", "Period is " + period);
        if (task != null){
            task.cancel();
            timer.purge();
        }
            task = new TimerTask() {
                @Override
                public void run() {
                    Logger.getInstance(NotificationService.this).log("Checking for notifications");
                    checkForNotifications(NotificationService.this);
                    lastUpdatedTime = System.currentTimeMillis();
                    updateServiceNotification();
                }
            };
            if (period > 0) {
                long delay = getDelay();
                long period2 = getPeriod();
                android.util.Log.d("StartForeground", "delay " + delay + " period " + period2);
                Logger.getInstance(NotificationService.this).log("Scheduling with delay " + delay + " and period " + period2);
                timer.schedule(task, getDelay(), getPeriod());
            }
    }

    private long getDelay(){
        android.util.Log.d("StartForeground", "Last updated = " + lastUpdatedTime);
        if (lastUpdatedTime == 0){
            return 0;
        }
        long period = getPeriod();
        long elapsedTime = System.currentTimeMillis() - lastUpdatedTime;
        return Math.max(0, period - elapsedTime);
    }

    private long getPeriod(){
        if (isCharging){
            return pluggedInDelay;
        }else{
            return batteryDelay;
        }

    }


    boolean isCharging;
    BroadcastReceiver powerBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


//            BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            boolean isCharging = intent.getIntExtra(EXTRA_PLUGGED, -1 ) > 0 ;
            android.util.Log.d("StartForeground", "Receiving " + intent.getAction() + " " + isCharging + " " + intent.getIntExtra(EXTRA_PLUGGED, -1 ));
            if (isCharging != NotificationService.this.isCharging) {
                NotificationService.this.isCharging = isCharging;
                updateServiceNotification();
                updateTimer();
            }
        }
    };

    private long lastUpdatedTime;

    Timer timer = new Timer();

    private void updateTimer() {
        android.util.Log.d("StartForeground", "Updating");
        if (task != null) {
            task.cancel();
            task = null;
            timer.purge();
            startTimer();
        }
    }

    private Notification updateServiceNotification() {
        String periodString = DateUtils.formatElapsedTime(getPeriod() / 1000);
        String lastUpdated = "Never";
        if (lastUpdatedTime > 0) {
            lastUpdated = DateUtils.formatSameDayTime( lastUpdatedTime, System.currentTimeMillis(), MEDIUM, MEDIUM).toString();
        }
        String powerString = this.isCharging ? " when plugged In" : " when on battery";

        android.util.Log.d("DateNotif", "periodString " + periodString + " lastUpdated " + lastUpdated);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), SERVICE_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ivelt_logo48)
                .setContentTitle("Notifications updated at: " + lastUpdated )
                .setContentText("Updates every " + periodString + powerString)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        Notification notification = notificationBuilder.build();
        NotificationManagerCompat.from(this).notify(SERVICE_NOTIFICATION_ID, notification);
        return notification;
    }

    private void stopForeground(){
        super.stopForeground(false);
        if (task != null) {
            task.cancel();
            task = null;
            timer.purge();
        }
        updateServiceNotification();
        try {
            unregisterReceiver(powerBroadcastReceiver);
        }catch (IllegalArgumentException ise){
            Logger.getInstance(this).log("Unregister receiver failed", ise);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            if (intent.getAction().equals(ACTION_CHECK_NOTIFICATIONS)){
                checkForNotifications(getApplicationContext());
            }else if (intent.getAction().equals(ACTION_UPDATE_DELAY_TIME)){
                updateCheckTimes();
            }
        }
        return super.onStartCommand(intent, flags, startId);
    }

    private void updateCheckTimes() {
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(this);
        boolean paused = preferences.getBoolean("pause_notifications", false);
        String plugged_in_delay = preferences.getString("plugged_in", "Never");
        String battery_delay = preferences.getString("battery", "Never");
        pluggedInDelay = convertDurationStringToMilliSeconds(plugged_in_delay);
        batteryDelay = convertDurationStringToMilliSeconds(battery_delay);
        Logger.getInstance(getApplicationContext()).log("AppSettings: paused " + paused + " plugged_in_delay " + pluggedInDelay + " battery " + batteryDelay);
        boolean useForeground = shouldStartForeground(paused, pluggedInDelay, batteryDelay);
        if (useForeground) {
            android.util.Log.d("StartForground", "Calling start");
            startForeground();
        }else{
            stopForeground();
        }
        if (shouldUseWorkManager(useForeground, paused, pluggedInDelay, batteryDelay)){
            checkForNotifications(getApplicationContext());
            startWorkManagers(pluggedInDelay, batteryDelay);
        }else{
            cancelWork();
        }
    }

    private void cancelWork() {
        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.cancelUniqueWork("Power");
        workManager.cancelUniqueWork("Battery");
        WorkManager.getInstance(getApplicationContext()).pruneWork();
    }

    private void startWorkManagers(int pluggedInDelay, int batteryDelay){
        if (pluggedInDelay >= 15 * MINUTE_IN_MILLIS){
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(PowerNotificationUpdater.class, pluggedInDelay , TimeUnit.MILLISECONDS)
                    .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresCharging(true).build())
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("Power", ExistingPeriodicWorkPolicy.REPLACE, workRequest);
            WorkManager.getInstance(getApplicationContext()).pruneWork();
            Logger.getInstance(getApplicationContext()).log("WMNS plugged in delay enqued");
        }else{
            WorkManager.getInstance(getApplicationContext()).cancelUniqueWork("Power");
        }
        if (batteryDelay >= 15 * MINUTE_IN_MILLIS){
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(BatteryNotificationUpdater.class, pluggedInDelay , TimeUnit.MILLISECONDS)
                    .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresCharging(false).build())
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("Battery", ExistingPeriodicWorkPolicy.REPLACE, workRequest);
            WorkManager.getInstance(getApplicationContext()).pruneWork();
            Logger.getInstance(getApplicationContext()).log("WMNS plugged in delay enqued");
        }else{
            WorkManager.getInstance(getApplicationContext()).cancelUniqueWork("Battery");
        }
    }

    private boolean shouldUseWorkManager(boolean useForeground, boolean paused, int pluggedInDelay, int batteryDelay) {
        if (paused || useForeground){
            return  false;
        }
        return  (pluggedInDelay + batteryDelay > 0);
    }

}
