package com.android.cts.ivelt;

import android.content.Context;
import android.os.BatteryManager;
import android.os.PowerManager;

import androidx.annotation.NonNull;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

public class BatteryNotificationUpdater extends Worker {
    public BatteryNotificationUpdater(@NonNull Context context, @NonNull  WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @NonNull
    @Override
    public Result doWork() {
        BatteryManager batteryManager = (BatteryManager) getApplicationContext().getSystemService(Context.BATTERY_SERVICE);
        if (!batteryManager.isCharging()){
            NotificationService.checkForNotifications(getApplicationContext());
        }
        return Result.success();
    }
}
