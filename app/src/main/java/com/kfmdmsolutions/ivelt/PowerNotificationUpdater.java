package com.kfmdmsolutions.ivelt;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;



public class PowerNotificationUpdater extends Worker {
    public PowerNotificationUpdater(@NonNull Context context, @NonNull  WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
//        BatteryManager batteryManager = (BatteryManager) getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
//        if (batteryManager.isCharging()){
        android.util.Log.d("NotifUpdate", "power work");
            NotificationService.checkForNotifications(getApplicationContext());
//        }
        return Result.success();
    }
}
