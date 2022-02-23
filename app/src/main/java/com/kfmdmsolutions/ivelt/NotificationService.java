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
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.service.notification.StatusBarNotification;
import android.text.Html;
import android.text.format.DateUtils;

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
import com.kfmdmsolutions.ivelt.Utilities.WebkitCookieManagerProxy;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static android.content.Intent.ACTION_BATTERY_CHANGED;
import static android.os.BatteryManager.EXTRA_PLUGGED;
import static com.kfmdmsolutions.ivelt.MainActivity.EXTRA_URL;
import static java.text.DateFormat.MEDIUM;

/**
 * An {@link IntentService} subclass for handling asynchronous task requests in
 * a service on a separate handler thread.
 * <p>
 * <p>
 * helper methods.
 */
public class NotificationService extends Service {

    public static final WebkitCookieManagerProxy coreCookieManager = new WebkitCookieManagerProxy(null, java.net.CookiePolicy.ACCEPT_ALL);
    public static final String DIRECT_MESSAGES_NOTIFICATION_CHANNEL = "DM";
    public static final String QUOTES_NOTIFICATION_CHANNEL = "Quotes";
    public static final String BOOKMARK_NOTIFICATION_CHANNEL = "BM";
    public static final String OTHER_NOTIFICATION_CHANNEL = "other";
    public static final String SERVICE_NOTIFICATION_CHANNEL = "service";
    public static final String SENT_NOTIFICATION_LIST = "SentNotificationList";
    public static NotificationService instance;

    public static final String ACTION_UPDATE_DELAY_TIME = "com.android.cts.ivelt.notification.service.action.update.delay.time";
    public static final String ACTION_CHECK_NOTIFICATIONS = "com.android.cts.ivelt.notification.service.action.check.notifications";
    public static final String ACTION_SAVE_NOTIFICATION_LIST = "com.android.cts.ivelt.notification.service.action.save.notification.list";
    public static final String ACTION_UPDATE_NOTIFICATIONS = "com.android.cts.ivelt.notification.service.action.update.notifications";
    public static final String ACTION_DISMISS_NOTIFICATION = "com.android.kfmdm.notification.service.action.dismiss.notification";
    public static final String ACTION_MARK_NOTIFICATION_READ = "com.android.kfmdm.notification.service.action.mark.notification.read";
    public static final String ACTION_PAUSE_NOTIFICATIONS= "com.android.kfmdm.notification.service.action.pause.notifications";
    public static final String ACTION_MARK_SIGN_OUT = "com.android.kfmdm.notification.service.action.mark.sign.out";

    public static final String EXTRA_NOTIFICATION_ID = "com.android.kfmdm.notification.service.extra.notification.id";

    public static final int ID_PAUSE_NOTIFICATIONS = 300;
    public static final int ID_SIGN_OUT = 301;

    private int pluggedInDelay;
    private int batteryDelay;

    private static ConcurrentLinkedQueue<Long> sentNotificationQueue;
    private Runnable runnable;
    private PowerManager.WakeLock wakeLock;

    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }

    public static void initChannels(Context context) {
        createNotificationChannel(context, "Direct Messages", "Show a notification when you get a direct message", DIRECT_MESSAGES_NOTIFICATION_CHANNEL);
        createNotificationChannel(context, "Quotes", "Show a notification when someone quotes you", QUOTES_NOTIFICATION_CHANNEL);
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
            int importance = channelID.equals(SERVICE_NOTIFICATION_CHANNEL) ? NotificationManager.IMPORTANCE_MIN : NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel channel = new NotificationChannel(channelID, name, importance);
            if (channelID.equals(SERVICE_NOTIFICATION_CHANNEL)) {
                channel.setShowBadge(false);
            }
            channel.setDescription(description);
            // Register the channel with the system; you can't change the importance
            // or other notification behaviors after this
            if (context != null) {
                NotificationManager notificationManager = (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    public static void startNotificationService(Context context, String action) {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(action);
        context.startService(intent);
    }

    public static void checkNotifications(Context context) {
        Intent intent = new Intent(context, NotificationService.class);
        intent.setAction(ACTION_CHECK_NOTIFICATIONS);
        context.startService(intent);
    }

    public static void checkForNotifications(Context context) {

        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connectivityManager.getActiveNetworkInfo();
        boolean connected = networkInfo != null && networkInfo.isConnected();
        if (!connected) {
            Logger.getInstance(context).log("Not checking for updates, not connected to internet");
            return;
        }
        String url = "http://www.ivelt.com/forum/ucp.php?i=ucp_notifications";
        Utils.executeAsync(() -> {
            if (sentNotificationQueue == null){
                initializeSNQ(context);
                if (sentNotificationQueue == null){
                    sentNotificationQueue = new ConcurrentLinkedQueue<>();
                }
            }
            try {
                Document doc = Utils.getConnection(url, null, context).get();
                Elements notificationList = doc.select(".notification_list");
                // URL was wrong.
                if (notificationList.isEmpty()){
                    loginAndRetry(context);
                }else {
                    parseNotificationPage(context, notificationList);
                }
            } catch (IOException e) {
                Logger.getInstance(context).log("Checking for notifications failed", e);
                e.printStackTrace();
            }
        });
    }

    private static void loginAndRetry(Context context) throws  IOException{
        IveltWebInterface iveltWebInterface = new IveltWebInterface(context);
        Document doc = Utils.getConnection("https://www.ivelt.com/forum/ucp.php?mode=login", null, context)
                .data("username", iveltWebInterface.getUsername())
                .data("password", iveltWebInterface.getPassword())
                .data("login", "android kfmdm app")
                .data("redirect", "./ucp.php?i=ucp_notifications")
                .data("autologin", "on")
                .followRedirects(true)
                .post();
        Elements notificationList = doc.select(".notification_list");
        if (!notificationList.isEmpty()) {
           parseNotificationPage(context, notificationList);
       }
    }

    private static void parseNotificationPage(Context context, Elements notificationList) {
        Elements newNotifications = notificationList.select(".row.bg3");
        Elements notifications = newNotifications.select(".notifications");
        List<Integer> ids = new ArrayList<>();
        for (Element element : notifications) {
            NotificationInfo info = new NotificationInfo(element);
            Logger.getInstance(context).log("notificationInfo url is " + info.url + " id is " + info.id);
            ids.add((int) info.id);
            showNotification(context, info);
        }
        NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        List<Integer> shownNotificationIDs = Arrays.stream(manager.getActiveNotifications()).map(StatusBarNotification::getId).collect(Collectors.toList());
        for (Integer id : shownNotificationIDs){
            if (!ids.contains(id)){
                manager.cancel(id);
            }
        }
        if (notifications.isEmpty()) {
            Logger.getInstance(context).log("Do we have notifications? " + (notificationList.first() != null));
        }
        Logger.getInstance(context).log("Notifications found: " + notifications.size());
    }

    private static void showNotification(Context context, NotificationInfo notificationInfo) {
        if (sentNotificationQueue.contains(notificationInfo.id)) {
            Logger.getInstance(context).log("Notification with ID " + notificationInfo.id + " already shown");
            return;
        }
        PendingIntent pendingIntent = getPendingIntent(context, notificationInfo);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(context.getApplicationContext(), notificationInfo.channelID)
                .setSmallIcon(R.drawable.ivelt_logo48)
                .setContentTitle(notificationInfo.title)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .addAction(new NotificationCompat.Action(null, context.getString(R.string.dismiss), getDismissIntent(context, notificationInfo)))
                .addAction(new NotificationCompat.Action(null, context.getString(R.string.mark_read), getMarkReadIntent(context, notificationInfo)))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(notificationInfo.text))
                .setPriority(NotificationCompat.PRIORITY_MAX);
        NotificationManagerCompat.from(context.getApplicationContext()).notify((int) notificationInfo.id, notificationBuilder.build());
        sentNotificationQueue.add(notificationInfo.id);
        if (sentNotificationQueue.size() > 50) {
            sentNotificationQueue.remove();
        }
        Logger.getInstance(context).log("Sent notification with ID " + notificationInfo.id);
    }

    private static PendingIntent getSettingsIntent(Context context){
        Intent intent = new Intent(context.getApplicationContext(), SettingsActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context.getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(intent);
        return stackBuilder.getPendingIntent(10002, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent getLogoutIntent(Context context){
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        intent.setAction( "com.kfmdm.ivelt.shortcut.logout");
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context.getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(intent);
        return stackBuilder.getPendingIntent(10003, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent getPendingIntent(Context context, NotificationInfo info) {
        String openNotification = context.getString(R.string.open_notification);
        String preference = PreferenceManager.getDefaultSharedPreferences(context).getString("click", openNotification);
        Intent intent = new Intent(context.getApplicationContext(), MainActivity.class);
        int id = 10001;
        if (info != null) {
                intent.putExtra(EXTRA_URL, info.url);
            id = (int) info.id;
        }
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(context.getApplicationContext());
        stackBuilder.addNextIntentWithParentStack(intent);
        return stackBuilder.getPendingIntent(id, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
    }


    enum NotificationType {
        PRIVATE_MESSAGE(DIRECT_MESSAGES_NOTIFICATION_CHANNEL), QUOTE(QUOTES_NOTIFICATION_CHANNEL), BOOKMARK(BOOKMARK_NOTIFICATION_CHANNEL), OTHER(OTHER_NOTIFICATION_CHANNEL);

        String channelID;

        NotificationType(String channelID) {
            this.channelID = channelID;
        }

        static NotificationType findType(String name) {
            if (name == null) {
                return OTHER;
            }
            switch (name.trim()) {
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
        Thread.UncaughtExceptionHandler defaultUncaughtExceptionHandler = Thread.getDefaultUncaughtExceptionHandler();
        Thread.setDefaultUncaughtExceptionHandler((thread, exception) -> {
            Logger.getInstance(this).log("Fatal Error", exception);
            if (defaultUncaughtExceptionHandler != null){
                defaultUncaughtExceptionHandler.uncaughtException(thread, exception);
            }
            System.exit(2);
        });
        handler = new Handler(Looper.myLooper());
        Utils.executeAsync(() -> initializeSNQ(this));
    }

    private static void initializeSNQ(Context context) {
        try {
            FileInputStream fis = context.openFileInput(SENT_NOTIFICATION_LIST);
            ObjectInputStream is = new ObjectInputStream(fis);
            Object object = is.readObject();
            android.util.Log.d("SNQ", " object is " + object.getClass());
            sentNotificationQueue = (ConcurrentLinkedQueue<Long>) object;
            android.util.Log.d("SNQ", " deserialzed " + sentNotificationQueue);
            is.close();
            fis.close();
        } catch (NullPointerException| IOException | ClassNotFoundException | ClassCastException e) {
            sentNotificationQueue = new ConcurrentLinkedQueue<>();
            Logger.getInstance(context).log("unable to deserialize sent notification queue, created a new one");

            android.util.Log.d("SNQ", " error derserializen ", e);
            e.printStackTrace();
        }
    }

    public static class NotificationInfo {
        NotificationType type;
        CharSequence title;
        String channelID;
        CharSequence text;
        String url;
        long id;

        NotificationInfo(Element notificationElement) {

            Elements link = notificationElement.getElementsByTag("a");
            if (link.first() != null) {
                String url = link.first().attr("href");
                android.util.Log.d("Notif", url);
                this.id = extractIDFromURL(url);
//                this.url = url;
                this.url = fixURL(url);

            } else {
                android.util.Log.d("Notif", "url null");
            }
            Elements title = notificationElement.getElementsByClass("notifications_title");
            if (title.first() != null) {
                Elements strong = title.first().getElementsByTag("strong");
                if (strong.first() != null) {
                    this.title = Html.fromHtml(strong.html(), 0).toString();
                    this.type = NotificationType.findType(this.title.toString());
                    this.channelID = this.type.channelID;
                    android.util.Log.d("Notif", "title " + this.title + " type " + this.type);
                }
                this.text = Html.fromHtml(title.first().html(), 0);
            }
        }

        private String fixURL(String url) {
            return url.replace("./", "https://www.ivelt.com/forum/");
        }

        public static long extractIDFromURL(String url) {

            Pattern pattern = Pattern.compile("mark_notification=([^;]*)&");
            Matcher matcher = pattern.matcher(url);
            if (matcher.find()) {
                return Long.parseLong(matcher.group().replaceAll("[^\\d]", ""));
            }
            return -1L;
        }
    }

    private boolean shouldStartForeground(boolean paused, int pluggedInDelay, int batteryDelay) {

        // using plus , will be 0 if both are 0, if only one is 0, it will be the other number, and if both are non 0, it will be some unknown number
        int delayTime = pluggedInDelay + batteryDelay;

        // both are 0 return false, or when notifications are paused.
        if (paused || ((delayTime) == 0)) {
            return false;
        }
        // Both are non 0, delay time is some unknown number, we need to set it the minimum delay time.
        if (((long)pluggedInDelay * batteryDelay) > 0L) {
            delayTime = Math.min(pluggedInDelay, batteryDelay);
            android.util.Log.d("AppSettings:", delayTime + "");
        }
        return ((delayTime < 15 * MINUTE_IN_MILLIS));
    }



    @Override
    public void onDestroy() {
        Logger.getInstance(NotificationService.this).log("StartForeground: Stopping");
        saveNotificationList();
        super.onDestroy();
    }

    private void saveNotificationList() {
        try {
            FileOutputStream fos = openFileOutput(SENT_NOTIFICATION_LIST, Context.MODE_PRIVATE);
            ObjectOutputStream os = new ObjectOutputStream(fos);
            os.writeObject(sentNotificationQueue);
            os.close();
            fos.close();
        } catch (IOException e) {

            android.util.Log.d("SNQ", "unable to save ", e);
        }
    }

    private static final int MINUTE_IN_MILLIS = 60 * 1000;
    private static final int HOUR_IN_MILLIS = 60 * MINUTE_IN_MILLIS;

    private int convertDurationStringToMilliSeconds(String duration) {
        duration = duration == null ? "" : duration;
        switch (duration) {
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


    Handler handler;
    private static final int SERVICE_NOTIFICATION_ID = 1000;

    private void startForeground() {

        PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
        if (wakeLock == null) {
            wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK,
                    "Ivelt::notificationWakeLock");
            wakeLock.acquire();
        }
        BatteryManager batteryManager = (BatteryManager) getSystemService(BATTERY_SERVICE);
        Notification notification = updateServiceNotification();
        startForeground(SERVICE_NOTIFICATION_ID, notification);
        registerReceiver(powerBroadcastReceiver, new IntentFilter(ACTION_BATTERY_CHANGED));
        isCharging = batteryManager.isCharging();

//        Looper.prepare();
        startTimer();
    }


//    TimerTask task;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    ScheduledFuture<?> scheduledFuture;

    private void startTimer() {
        long period = getPeriod();
        if (scheduledFuture != null) {
            Logger.getInstance(NotificationService.this).log("StartForeground: Cancelling Before starting");
            scheduledFuture.cancel(false);
        }
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        if (period > 0) {
            runnable = new Runnable() {
                @Override
                public void run() {
                    Logger.getInstance(NotificationService.this).log("Checking for notifications");
                    checkForNotifications(NotificationService.this);
                    lastUpdatedTime = System.currentTimeMillis();
                    updateServiceNotification();
                    Logger.getInstance(NotificationService.this).log("Scheduling next check in " + period);
                    handler.postDelayed(this, period);
                }
            };
            long delay = getDelay();
            Logger.getInstance(NotificationService.this).log("StartForeground: Scheduling with delay " + delay + " period " + period + " Powered " + isCharging);
            handler.postDelayed(runnable, delay);
        }
        updateServiceNotification();
    }

    private long getDelay() {
        android.util.Log.d("StartForeground", "Last updated = " + lastUpdatedTime);
        if (lastUpdatedTime == 0) {
            return 0;
        }
        long period = getPeriod();
        long elapsedTime = System.currentTimeMillis() - lastUpdatedTime;
        return Math.max(0, period - elapsedTime);
    }

    private long getPeriod() {
        if (isCharging) {
            return pluggedInDelay;
        } else {
            return batteryDelay;
        }

    }


    boolean isCharging;
    BroadcastReceiver powerBroadcastReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {


//            BatteryManager manager = (BatteryManager) getSystemService(BATTERY_SERVICE);
            boolean isCharging = intent.getIntExtra(EXTRA_PLUGGED, -1) > 0;
            android.util.Log.d("StartForeground", "Receiving " + intent.getAction() + " " + isCharging + " " + intent.getIntExtra(EXTRA_PLUGGED, -1));
            if (isCharging != NotificationService.this.isCharging) {
                NotificationService.this.isCharging = isCharging;
                updateServiceNotification();
                updateTimer();
            }
        }
    };

    private long lastUpdatedTime;


    private void updateTimer() {
        if (scheduledFuture != null) {
            Logger.getInstance(NotificationService.this).log("StartForeground: Cancelling because updating");
            if (handler != null && runnable != null) {
                handler.removeCallbacks(runnable);
            }
//            scheduledFuture.cancel(false);
//            scheduledFuture = null;
            startTimer();
        }
    }

    private Notification updateServiceNotification() {
        String periodString = DateUtils.formatElapsedTime(getPeriod() / 1000);
        String lastUpdated = "Never";
        if (lastUpdatedTime > 0) {
            lastUpdated = DateUtils.formatSameDayTime(lastUpdatedTime, System.currentTimeMillis(), MEDIUM, MEDIUM).toString();
        }
        String powerString = this.isCharging ? " when plugged In" : " when on battery";

        android.util.Log.d("DateNotif", "periodString " + periodString + " lastUpdated " + lastUpdated);
        NotificationCompat.Builder notificationBuilder = new NotificationCompat.Builder(getApplicationContext(), SERVICE_NOTIFICATION_CHANNEL)
                .setSmallIcon(R.drawable.ivelt_logo48)
                .setUsesChronometer(true)
                .setWhen(lastUpdatedTime)
                .setSilent(true)
                .addAction(new NotificationCompat.Action(1, "Check Now", getUpdateIntent()))
                .addAction(new NotificationCompat.Action(1, "Settings", getSettingsIntent(this)))
                .addAction(new NotificationCompat.Action(1, "Logout", getLogoutIntent(this)))
                .setContentIntent(getPendingIntent(this, null))
                .setContentTitle("Notifications updated at: " + lastUpdated)
                .setContentText("Updates every " + periodString + powerString)
                .setPriority(NotificationCompat.PRIORITY_MIN);

        Notification notification = notificationBuilder.build();
        NotificationManagerCompat.from(this).notify(SERVICE_NOTIFICATION_ID, notification);
        return notification;
    }

    private PendingIntent getUpdateIntent() {
        Intent intent = new Intent(getApplicationContext(), NotificationService.class);
        intent.setAction(ACTION_UPDATE_NOTIFICATIONS);
        return PendingIntent.getService(getApplicationContext(), 2, intent, PendingIntent.FLAG_IMMUTABLE);
    }
    private static PendingIntent getDismissIntent(Context context, NotificationInfo info) {
        Intent intent = new Intent(context.getApplicationContext(), NotificationService.class);
        intent.setAction(ACTION_DISMISS_NOTIFICATION);

        android.util.Log.d("MNRID", "Putting id di " + info.id);
        intent.putExtra(EXTRA_NOTIFICATION_ID, info.id);
        return PendingIntent.getService(context.getApplicationContext(), (int) info.id, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private static PendingIntent getMarkReadIntent(Context context, NotificationInfo info) {
        Intent intent = new Intent(context.getApplicationContext(), NotificationService.class);
        intent.setAction(ACTION_MARK_NOTIFICATION_READ);
        android.util.Log.d("MNRID", "Putting id mri " + info.id);
        intent.putExtra(EXTRA_NOTIFICATION_ID, info.id);
        intent.putExtra(EXTRA_URL, info.url);
        return PendingIntent.getService(context.getApplicationContext(), (int) info.id, intent, PendingIntent.FLAG_IMMUTABLE);
    }

    private void stopForeground() {
        if (wakeLock != null) {
            wakeLock.release();
            wakeLock = null;
        }
        Logger.getInstance(NotificationService.this).log("StartForeground: Cancelling because stopping");
        if (handler != null && runnable != null) {
            handler.removeCallbacks(runnable);
        }
        try {
            unregisterReceiver(powerBroadcastReceiver);
        } catch (IllegalArgumentException ise) {
            Logger.getInstance(this).log("Unregister receiver failed, receiver not registered");
        }
        super.stopForeground(true);

    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        java.net.CookieHandler.setDefault(coreCookieManager);
        if (intent != null && intent.getAction() != null) {
            switch (intent.getAction()) {
                case ACTION_UPDATE_NOTIFICATIONS:
                    checkForNotifications(getApplicationContext());
                    lastUpdatedTime = System.currentTimeMillis();
                    updateServiceNotification();
                case ACTION_CHECK_NOTIFICATIONS:
                    checkForNotifications(getApplicationContext());
                    break;
                case ACTION_UPDATE_DELAY_TIME:
                    updateCheckTimes();
                    break;
                case ACTION_SAVE_NOTIFICATION_LIST:
                    saveNotificationList();
                    break;
                case ACTION_MARK_NOTIFICATION_READ:
                    String url = intent.getStringExtra(EXTRA_URL);
                    long mnrid = intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1);
                    android.util.Log.d("MNRID", mnrid + "");
                    if (url != null) {
                        Utils.executeAsync(() ->
                        {
                            try {
                                Utils.getConnection(url, null, this).get();
                                if (mnrid > 0){
                                    NotificationManagerCompat.from(getApplicationContext()).cancel((int) mnrid);
                                }
                            } catch (IOException e) {
                                e.printStackTrace();
                            }
                        });

                    }
                case ACTION_DISMISS_NOTIFICATION:
                    long id = intent.getLongExtra(EXTRA_NOTIFICATION_ID, -1);

                    android.util.Log.d("MNRID", id + "");
                    if (id > 0){
                        NotificationManagerCompat.from(getApplicationContext()).cancel((int) id);
                    }
                    break;

            }
        }
        return START_STICKY;
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
            android.util.Log.d("StartForeground", "Calling start");
            startForeground();
        } else {
            stopForeground();
        }
        if (shouldUseWorkManager(useForeground, paused, pluggedInDelay, batteryDelay)) {
            checkForNotifications(getApplicationContext());
            startWorkManagers(pluggedInDelay, batteryDelay);
        } else {
            cancelWork();
        }
    }

    private void cancelWork() {
        WorkManager workManager = WorkManager.getInstance(getApplicationContext());
        workManager.cancelUniqueWork("Power");
        workManager.cancelUniqueWork("Battery");
        WorkManager.getInstance(getApplicationContext()).pruneWork();
    }

    private void startWorkManagers(int pluggedInDelay, int batteryDelay) {
        if (pluggedInDelay >= 15 * MINUTE_IN_MILLIS) {
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(PowerNotificationUpdater.class, pluggedInDelay, TimeUnit.MILLISECONDS)
                    .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresCharging(true).build())
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("Power", ExistingPeriodicWorkPolicy.REPLACE, workRequest);
            WorkManager.getInstance(getApplicationContext()).pruneWork();
            Logger.getInstance(getApplicationContext()).log("WMNS plugged in delay enqued");
        } else {
            WorkManager.getInstance(getApplicationContext()).cancelUniqueWork("Power");
        }
        if (batteryDelay >= 15 * MINUTE_IN_MILLIS) {
            PeriodicWorkRequest workRequest = new PeriodicWorkRequest.Builder(BatteryNotificationUpdater.class, pluggedInDelay, TimeUnit.MILLISECONDS)
                    .setConstraints(new Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).setRequiresCharging(false).build())
                    .build();
            WorkManager.getInstance(getApplicationContext()).enqueueUniquePeriodicWork("Battery", ExistingPeriodicWorkPolicy.REPLACE, workRequest);
            WorkManager.getInstance(getApplicationContext()).pruneWork();
            Logger.getInstance(getApplicationContext()).log("WMNS plugged in delay enqued");
        } else {
            WorkManager.getInstance(getApplicationContext()).cancelUniqueWork("Battery");
        }
    }

    private boolean shouldUseWorkManager(boolean useForeground, boolean paused, int pluggedInDelay, int batteryDelay) {
        if (paused || useForeground) {
            return false;
        }
        return (pluggedInDelay + batteryDelay > 0);
    }

}
