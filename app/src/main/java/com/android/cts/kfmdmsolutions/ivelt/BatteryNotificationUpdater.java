package com.android.cts.kfmdmsolutions.ivelt;

import android.content.Context;
import android.os.BatteryManager;
import android.os.Build;

import androidx.annotation.NonNull;
import androidx.annotation.RequiresApi;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.android.cts.kfmdmsolutions.ivelt.Utilities.Logger;

public class BatteryNotificationUpdater extends Worker {
    public BatteryNotificationUpdater(@NonNull Context context, @NonNull  WorkerParameters workerParams) {
        super(context, workerParams);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @NonNull
    @Override
    public Result doWork() {
        BatteryManager batteryManager = (BatteryManager) getApplicationContext().getSystemService(Context.BATTERY_SERVICE);

        Logger.getInstance(getApplicationContext()).log("NotifUpdate Doing battery work isCharging? " + batteryManager.isCharging());
//        if (!batteryManager.isCharging()){
            NotificationService.checkForNotifications(getApplicationContext());
//        }
        return Result.success();
    }
}
